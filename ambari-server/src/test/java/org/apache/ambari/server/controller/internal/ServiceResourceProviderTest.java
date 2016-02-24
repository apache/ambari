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

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ServiceComponentHostRequest;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
import org.apache.ambari.server.controller.ServiceRequest;
import org.apache.ambari.server.controller.ServiceResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.metadata.RoleCommandOrder;
import org.apache.ambari.server.serveraction.kerberos.KerberosAdminAuthenticationException;
import org.apache.ambari.server.serveraction.kerberos.KerberosMissingAdminCredentialsException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.easymock.Capture;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

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
  public void testGetResources_KerberosSpecificProperties() throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service0 = createNiceMock(Service.class);
    ServiceResponse serviceResponse0 = createNiceMock(ServiceResponse.class);

    StackId stackId = createNiceMock(StackId.class);
    ServiceFactory serviceFactory = createNiceMock(ServiceFactory.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    KerberosHelper kerberosHeper = createStrictMock(KerberosHelper.class);

    Map<String, Service> allResponseMap = new HashMap<String, Service>();
    allResponseMap.put("KERBEROS", service0);

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getServiceFactory()).andReturn(serviceFactory).anyTimes();
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).
        andReturn(Collections.<ServiceComponentHostResponse>emptySet()).anyTimes();

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();

    expect(cluster.getServices()).andReturn(allResponseMap).anyTimes();
    expect(cluster.getService("KERBEROS")).andReturn(service0);

    expect(service0.convertToResponse()).andReturn(serviceResponse0).anyTimes();

    expect(service0.getName()).andReturn("Service100").anyTimes();

    expect(serviceResponse0.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(serviceResponse0.getServiceName()).andReturn("KERBEROS").anyTimes();

    kerberosHeper.validateKDCCredentials(cluster);

    // replay
    replay(managementController, clusters, cluster, service0, serviceResponse0,
        ambariMetaInfo, stackId, serviceFactory, kerberosHeper);

    ResourceProvider provider = getServiceProvider(managementController);
    // set kerberos helper on provider
    Class<?> c = provider.getClass();
    Field f = c.getDeclaredField("kerberosHelper");
    f.setAccessible(true);
    f.set(provider, kerberosHeper);

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID);
    propertyIds.add(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID);

    // create the request
    Predicate predicate = new PredicateBuilder().property(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").and().
        property(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).equals("KERBEROS").toPredicate();
    Request request = PropertyHelper.getReadRequest("ServiceInfo", "Services");
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      Assert.assertEquals("Cluster100", resource.getPropertyValue(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID));
      Assert.assertEquals("KERBEROS", resource.getPropertyValue(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID));
      Assert.assertEquals("OK", resource.getPropertyValue("Services/attributes/kdc_validation_result"));
      Assert.assertEquals("", resource.getPropertyValue("Services/attributes/kdc_validation_failure_details"));
    }

    // verify
    verify(managementController, clusters, cluster, service0, serviceResponse0,
        ambariMetaInfo, stackId, serviceFactory, kerberosHeper);
  }

  @Test
  public void testGetResources_KerberosSpecificProperties_NoKDCValidation() throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service0 = createNiceMock(Service.class);
    ServiceResponse serviceResponse0 = createNiceMock(ServiceResponse.class);

    StackId stackId = createNiceMock(StackId.class);
    ServiceFactory serviceFactory = createNiceMock(ServiceFactory.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    KerberosHelper kerberosHelper = createStrictMock(KerberosHelper.class);

    Map<String, Service> allResponseMap = new HashMap<String, Service>();
    allResponseMap.put("KERBEROS", service0);

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getServiceFactory()).andReturn(serviceFactory).anyTimes();
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).
        andReturn(Collections.<ServiceComponentHostResponse>emptySet()).anyTimes();

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();

    expect(cluster.getServices()).andReturn(allResponseMap).anyTimes();
    expect(cluster.getService("KERBEROS")).andReturn(service0);

    expect(service0.convertToResponse()).andReturn(serviceResponse0).anyTimes();

    expect(service0.getName()).andReturn("Service100").anyTimes();

    expect(serviceResponse0.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(serviceResponse0.getServiceName()).andReturn("KERBEROS").anyTimes();

    // The following call should NOT be made
    // kerberosHelper.validateKDCCredentials(cluster);

    // replay
    replay(managementController, clusters, cluster, service0, serviceResponse0,
        ambariMetaInfo, stackId, serviceFactory, kerberosHelper);

    ResourceProvider provider = getServiceProvider(managementController);
    // set kerberos helper on provider
    Class<?> c = provider.getClass();
    Field f = c.getDeclaredField("kerberosHelper");
    f.setAccessible(true);
    f.set(provider, kerberosHelper);

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID);
    propertyIds.add(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID);

    // create the request
    Predicate predicate = new PredicateBuilder().property(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").and().
        property(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).equals("KERBEROS").toPredicate();
    Request request = PropertyHelper.getReadRequest("ServiceInfo");
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      Assert.assertEquals("Cluster100", resource.getPropertyValue(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID));
      Assert.assertEquals("KERBEROS", resource.getPropertyValue(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID));
    }

    // verify
    verify(managementController, clusters, cluster, service0, serviceResponse0,
        ambariMetaInfo, stackId, serviceFactory, kerberosHelper);
  }

  @Test
  public void testGetResources_KerberosSpecificProperties_KDCInvalidCredentials() throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service0 = createNiceMock(Service.class);
    ServiceResponse serviceResponse0 = createNiceMock(ServiceResponse.class);

    StackId stackId = createNiceMock(StackId.class);
    ServiceFactory serviceFactory = createNiceMock(ServiceFactory.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    KerberosHelper kerberosHeper = createStrictMock(KerberosHelper.class);

    Map<String, Service> allResponseMap = new HashMap<String, Service>();
    allResponseMap.put("KERBEROS", service0);

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getServiceFactory()).andReturn(serviceFactory).anyTimes();
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).
        andReturn(Collections.<ServiceComponentHostResponse>emptySet()).anyTimes();

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();

    expect(cluster.getServices()).andReturn(allResponseMap).anyTimes();
    expect(cluster.getService("KERBEROS")).andReturn(service0);

    expect(service0.convertToResponse()).andReturn(serviceResponse0).anyTimes();

    expect(service0.getName()).andReturn("Service100").anyTimes();

    expect(serviceResponse0.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(serviceResponse0.getServiceName()).andReturn("KERBEROS").anyTimes();

    kerberosHeper.validateKDCCredentials(cluster);
    expectLastCall().andThrow(new KerberosAdminAuthenticationException("Invalid KDC administrator credentials."));

    // replay
    replay(managementController, clusters, cluster, service0, serviceResponse0,
        ambariMetaInfo, stackId, serviceFactory, kerberosHeper);

    ResourceProvider provider = getServiceProvider(managementController);
    // set kerberos helper on provider
    Class<?> c = provider.getClass();
    Field f = c.getDeclaredField("kerberosHelper");
    f.setAccessible(true);
    f.set(provider, kerberosHeper);

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID);
    propertyIds.add(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID);

    // create the request
    Predicate predicate = new PredicateBuilder().property(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").and().
        property(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).equals("KERBEROS").toPredicate();
    Request request = PropertyHelper.getReadRequest("ServiceInfo", "Services");
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      Assert.assertEquals("Cluster100", resource.getPropertyValue(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID));
      Assert.assertEquals("KERBEROS", resource.getPropertyValue(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID));
      Assert.assertEquals("INVALID_CREDENTIALS", resource.getPropertyValue("Services/attributes/kdc_validation_result"));
      Assert.assertEquals("Invalid KDC administrator credentials.", resource.getPropertyValue("Services/attributes/kdc_validation_failure_details"));
    }

    // verify
    verify(managementController, clusters, cluster, service0, serviceResponse0,
        ambariMetaInfo, stackId, serviceFactory, kerberosHeper);
  }

  @Test
  public void testGetResources_KerberosSpecificProperties_KDCMissingCredentials() throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service0 = createNiceMock(Service.class);
    ServiceResponse serviceResponse0 = createNiceMock(ServiceResponse.class);

    StackId stackId = createNiceMock(StackId.class);
    ServiceFactory serviceFactory = createNiceMock(ServiceFactory.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    KerberosHelper kerberosHeper = createStrictMock(KerberosHelper.class);

    Map<String, Service> allResponseMap = new HashMap<String, Service>();
    allResponseMap.put("KERBEROS", service0);

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getServiceFactory()).andReturn(serviceFactory).anyTimes();
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).
        andReturn(Collections.<ServiceComponentHostResponse>emptySet()).anyTimes();

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();

    expect(cluster.getServices()).andReturn(allResponseMap).anyTimes();
    expect(cluster.getService("KERBEROS")).andReturn(service0);

    expect(service0.convertToResponse()).andReturn(serviceResponse0).anyTimes();

    expect(service0.getName()).andReturn("Service100").anyTimes();

    expect(serviceResponse0.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(serviceResponse0.getServiceName()).andReturn("KERBEROS").anyTimes();

    kerberosHeper.validateKDCCredentials(cluster);
    expectLastCall().andThrow(new KerberosMissingAdminCredentialsException("Missing KDC administrator credentials."));

    // replay
    replay(managementController, clusters, cluster, service0, serviceResponse0,
        ambariMetaInfo, stackId, serviceFactory, kerberosHeper);

    ResourceProvider provider = getServiceProvider(managementController);
    // set kerberos helper on provider
    Class<?> c = provider.getClass();
    Field f = c.getDeclaredField("kerberosHelper");
    f.setAccessible(true);
    f.set(provider, kerberosHeper);

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID);
    propertyIds.add(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID);

    // create the request
    Predicate predicate = new PredicateBuilder().property(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").and().
        property(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).equals("KERBEROS").toPredicate();
    Request request = PropertyHelper.getReadRequest("ServiceInfo", "Services");
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      Assert.assertEquals("Cluster100", resource.getPropertyValue(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID));
      Assert.assertEquals("KERBEROS", resource.getPropertyValue(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID));
      Assert.assertEquals("MISSING_CREDENTIALS", resource.getPropertyValue("Services/attributes/kdc_validation_result"));
      Assert.assertEquals("Missing KDC administrator credentials.", resource.getPropertyValue("Services/attributes/kdc_validation_failure_details"));
    }

    // verify
    verify(managementController, clusters, cluster, service0, serviceResponse0,
        ambariMetaInfo, stackId, serviceFactory, kerberosHeper);
  }


  @Test
  public void testUpdateResources() throws Exception{
    MaintenanceStateHelper maintenanceStateHelper = createNiceMock(MaintenanceStateHelper.class);
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service0 = createNiceMock(Service.class);
    ServiceFactory serviceFactory = createNiceMock(ServiceFactory.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    RequestStageContainer requestStages = createNiceMock(RequestStageContainer.class);
    RequestStatusResponse requestStatusResponse = createNiceMock(RequestStatusResponse.class);
    RoleCommandOrder rco = createNiceMock(RoleCommandOrder.class);

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

    expect(managementController.addStages((RequestStageContainer) isNull(), capture(clusterCapture), capture(requestPropertiesCapture),
        capture(requestParametersCapture), capture(changedServicesCapture), capture(changedCompsCapture),
        capture(changedScHostsCapture), capture(ignoredScHostsCapture), anyBoolean(), anyBoolean()
    )).andReturn(requestStages);
    requestStages.persist();
    expect(requestStages.getRequestStatusResponse()).andReturn(requestStatusResponse);
    expect(maintenanceStateHelper.isOperationAllowed(anyObject(Resource.Type.class), anyObject(Service.class))).andReturn(true).anyTimes();

    expect(service0.getCluster()).andReturn(cluster).anyTimes();
    expect(managementController.getRoleCommandOrder(cluster)).andReturn(rco).
    anyTimes();
    expect(rco.getTransitiveServices(eq(service0), eq(RoleCommand.START))).
    andReturn(Collections.<Service>emptySet()).anyTimes();

    // replay
    replay(managementController, clusters, cluster, rco, maintenanceStateHelper,
        service0, serviceFactory, ambariMetaInfo, requestStages, requestStatusResponse);

    ServiceResourceProvider provider = getServiceProvider(managementController, maintenanceStateHelper);

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
    verify(managementController, clusters, cluster, maintenanceStateHelper,
        service0, serviceFactory, ambariMetaInfo, requestStages, requestStatusResponse);
  }

  @Test
  public void testReconfigureClientsFlag() throws Exception {
    MaintenanceStateHelper maintenanceStateHelper = createNiceMock(MaintenanceStateHelper.class);
    AmbariManagementController managementController1 = createMock(AmbariManagementController.class);
    AmbariManagementController managementController2 = createMock
        (AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service0 = createNiceMock(Service.class);
    ServiceResponse serviceResponse0 = createNiceMock(ServiceResponse.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    RequestStageContainer requestStages1 = createNiceMock(RequestStageContainer.class);
    RequestStageContainer requestStages2 = createNiceMock(RequestStageContainer.class);

    RequestStatusResponse response1 = createNiceMock(RequestStatusResponse.class);
    RequestStatusResponse response2 = createNiceMock(RequestStatusResponse
      .class);
    RoleCommandOrder rco = createNiceMock(RoleCommandOrder.class);

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

    expect(managementController1.addStages((RequestStageContainer) isNull(), capture(clusterCapture), capture(requestPropertiesCapture),
        capture(requestParametersCapture), capture(changedServicesCapture), capture(changedCompsCapture),
        capture(changedScHostsCapture), capture(ignoredScHostsCapture), anyBoolean(), anyBoolean()
    )).andReturn(requestStages1);

    expect(managementController2.addStages((RequestStageContainer) isNull(), capture(clusterCapture), capture(requestPropertiesCapture),
        capture(requestParametersCapture), capture(changedServicesCapture), capture(changedCompsCapture),
        capture(changedScHostsCapture), capture(ignoredScHostsCapture), anyBoolean(), anyBoolean()
    )).andReturn(requestStages2);

    requestStages1.persist();
    expect(requestStages1.getRequestStatusResponse()).andReturn(response1);

    requestStages2.persist();
    expect(requestStages2.getRequestStatusResponse()).andReturn(response2);

    expect(maintenanceStateHelper.isOperationAllowed(anyObject(Resource.Type.class), anyObject(Service.class))).andReturn(true).anyTimes();

    expect(service0.getCluster()).andReturn(cluster).anyTimes();
    expect(managementController1.getRoleCommandOrder(cluster)).andReturn(rco).
    anyTimes();
    expect(managementController2.getRoleCommandOrder(cluster)).andReturn(rco).
    anyTimes();
    expect(rco.getTransitiveServices(eq(service0), eq(RoleCommand.START))).
    andReturn(Collections.<Service>emptySet()).anyTimes();

    // replay
    replay(managementController1, response1, managementController2, requestStages1, requestStages2, response2,
        clusters, cluster, service0, serviceResponse0, ambariMetaInfo, rco, maintenanceStateHelper);

    ServiceResourceProvider provider1 = getServiceProvider(managementController1, maintenanceStateHelper);

    ServiceResourceProvider provider2 = getServiceProvider(managementController2, maintenanceStateHelper);

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
    verify(managementController1, response1, managementController2, requestStages1, requestStages2, response2,
        clusters, cluster, service0, serviceResponse0, ambariMetaInfo, maintenanceStateHelper);
  }

  @Test
  public void testDeleteResources() throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    
    String serviceName = "Service100";

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(cluster.getService(serviceName)).andReturn(service).anyTimes();
    expect(service.getDesiredState()).andReturn(State.INSTALLED).anyTimes();
    expect(service.getName()).andReturn(serviceName).anyTimes();
    expect(service.getServiceComponents()).andReturn(new HashMap<String, ServiceComponent>());
    expect(service.getCluster()).andReturn(cluster);
    cluster.deleteService(serviceName);

    // replay
    replay(managementController, clusters, cluster, service);

    ResourceProvider provider = getServiceProvider(managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();

    ((ObservableResourceProvider)provider).addObserver(observer);

    // delete the service named Service100
    Predicate  predicate = new PredicateBuilder().property(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").and()
        .property(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).equals(serviceName).toPredicate();
    provider.deleteResources(predicate);


    ResourceProviderEvent lastEvent = observer.getLastEvent();
    Assert.assertNotNull(lastEvent);
    Assert.assertEquals(Resource.Type.Service, lastEvent.getResourceType());
    Assert.assertEquals(ResourceProviderEvent.Type.Delete, lastEvent.getType());
    Assert.assertEquals(predicate, lastEvent.getPredicate());
    Assert.assertNull(lastEvent.getRequest());

    // verify
    verify(managementController, clusters, cluster, service);
  }

  @Test
  public void testDeleteResourcesBadServiceState() throws Exception {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    
    String serviceName = "Service100";

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(cluster.getService(serviceName)).andReturn(service).anyTimes();
    expect(service.getDesiredState()).andReturn(State.STARTED).anyTimes();
    expect(service.getName()).andReturn(serviceName).anyTimes();

    // replay
    replay(managementController, clusters, cluster, service);

    ResourceProvider provider = getServiceProvider(managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();

    ((ObservableResourceProvider)provider).addObserver(observer);

    // delete the service named Service100
    Predicate  predicate = new PredicateBuilder().property(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").and()
        .property(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).equals(serviceName).toPredicate();
    
    try {
      provider.deleteResources(predicate);
      Assert.fail("Expected exception deleting a service in a non-removable state.");
    } catch (SystemException e) {
      // expected
    }
  }  

  @Test
  public void testDeleteResourcesBadComponentState() throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent sc = createNiceMock(ServiceComponent.class);
    Map<String, ServiceComponent> scMap = new HashMap<String, ServiceComponent>();
    scMap.put("Component100", sc);
    
    
    String serviceName = "Service100";

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(cluster.getService(serviceName)).andReturn(service).anyTimes();
    expect(service.getDesiredState()).andReturn(State.INSTALLED).anyTimes();
    expect(service.getName()).andReturn(serviceName).anyTimes();
    expect(service.getServiceComponents()).andReturn(scMap);
    expect(sc.getDesiredState()).andReturn(State.STARTED);

    // replay
    replay(managementController, clusters, cluster, service, sc);

    ResourceProvider provider = getServiceProvider(managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();

    ((ObservableResourceProvider)provider).addObserver(observer);

    // delete the service named Service100
    Predicate  predicate = new PredicateBuilder().property(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").and()
        .property(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).equals(serviceName).toPredicate();
    
    try {
      provider.deleteResources(predicate);
      Assert.fail("Expected exception deleting a service in a non-removable state.");
    } catch (SystemException e) {
      // expected
    }
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

    MaintenanceStateHelper maintenanceStateHelperMock = createNiceMock(MaintenanceStateHelper.class);
    AbstractResourceProvider provider = new ServiceResourceProvider(propertyIds,
        keyPropertyIds,
        managementController, maintenanceStateHelperMock);

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

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("C1", "MAPREDUCE", "JOBTRACKER", "JOBTRACKER", "Host100",
        "STARTED", "", null, null, null);
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("C1", "MAPREDUCE", "MAPREDUCE_CLIENT", "MAPREDUCE_CLIENT", "Host100", "STARTED", "", null, null, null);
    ServiceComponentHostResponse shr3 = new ServiceComponentHostResponse("C1", "MAPREDUCE", "TASKTRACKER", "TASKTRACKER", "Host100", "STARTED", "", null, null, null);

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


    expect(ambariMetaInfo.getComponent((String) anyObject(), (String) anyObject(),
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

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("C1", "MAPREDUCE", "JOBTRACKER", "JOBTRACKER", "Host100", "UNKNOWN", "", null, null, null);
    shr1.setMaintenanceState(MaintenanceState.OFF.toString());
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("C1", "MAPREDUCE", "MAPREDUCE_CLIENT", "MAPREDUCE_CLIENT", "Host100", "STARTED", "", null, null, null);
    shr2.setMaintenanceState(MaintenanceState.OFF.toString());
    ServiceComponentHostResponse shr3 = new ServiceComponentHostResponse("C1", "MAPREDUCE", "TASKTRACKER", "TASKTRACKER", "Host100", "STARTED", "", null, null, null);
    shr3.setMaintenanceState(MaintenanceState.OFF.toString());

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


    expect(ambariMetaInfo.getComponent((String) anyObject(), (String) anyObject(),
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

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("C1", "MAPREDUCE", "JOBTRACKER", "JOBTRACKER", "Host100", "STARTING", "", null, null, null);
    shr1.setMaintenanceState(MaintenanceState.OFF.toString());
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("C1", "MAPREDUCE", "MAPREDUCE_CLIENT", "MAPREDUCE_CLIENT", "Host100", "STARTED", "", null, null, null);
    shr2.setMaintenanceState(MaintenanceState.OFF.toString());
    ServiceComponentHostResponse shr3 = new ServiceComponentHostResponse("C1", "MAPREDUCE", "TASKTRACKER", "TASKTRACKER", "Host100", "STARTED", "", null, null, null);
    shr3.setMaintenanceState(MaintenanceState.OFF.toString());

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


    expect(ambariMetaInfo.getComponent((String) anyObject(), (String) anyObject(),
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

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("C1", "MAPREDUCE", "JOBTRACKER", "JOBTRACKER", "Host100", "INSTALLED", "", null, null, null);
    shr1.setMaintenanceState(MaintenanceState.OFF.toString());
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("C1", "MAPREDUCE", "MAPREDUCE_CLIENT", "MAPREDUCE_CLIENT", "Host100", "STARTED", "", null, null, null);
    shr2.setMaintenanceState(MaintenanceState.OFF.toString());
    ServiceComponentHostResponse shr3 = new ServiceComponentHostResponse("C1", "MAPREDUCE", "TASKTRACKER", "TASKTRACKER", "Host100", "STARTED", "", null, null, null);
    shr3.setMaintenanceState(MaintenanceState.OFF.toString());

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


    expect(ambariMetaInfo.getComponent((String) anyObject(), (String) anyObject(),
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
  public void testDefaultServiceState_DISABLED() throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    StackId stackId = createNiceMock(StackId.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("C1", "MAPREDUCE", "JOBTRACKER", "JOBTRACKER", "Host100", "DISABLED", "", null, null, null);
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("C1", "MAPREDUCE", "MAPREDUCE_CLIENT", "MAPREDUCE_CLIENT", "Host100", "STARTED", "", null, null, null);
    ServiceComponentHostResponse shr3 = new ServiceComponentHostResponse("C1", "MAPREDUCE", "TASKTRACKER", "TASKTRACKER", "Host100", "STARTED", "", null, null, null);

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


    expect(ambariMetaInfo.getComponent((String) anyObject(), (String) anyObject(),
        (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();

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

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("C1", "HDFS", "NAMENODE", "NAMENODE", "Host100",  "STARTED", "", null, null, null);
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("C1", "HDFS", "SECONDARY_NAMENODE", "SECONDARY_NAMENODE", "Host100", "STARTED", "", null, null, null);
    ServiceComponentHostResponse shr3 = new ServiceComponentHostResponse("C1", "HDFS", "JOURNALNODE", "JOURNALNODE", "Host100", "STARTED", "", null, null, null);

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


    expect(ambariMetaInfo.getComponent((String) anyObject(), (String) anyObject(),
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

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("C1", "HDFS", "NAMENODE", "NAMENODE", "Host100", "INSTALLED", "", null, null, null);
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("C1", "HDFS", "NAMENODE", "NAMENODE", "Host101", "STARTED", "", null, null, null);
    ServiceComponentHostResponse shr3 = new ServiceComponentHostResponse("C1", "HDFS", "JOURNALNODE", "JOURNALNODE", "Host100", "STARTED", "", null, null, null);

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


    expect(ambariMetaInfo.getComponent((String) anyObject(), (String) anyObject(),
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
  public void testHiveServiceState_INSTALLED() throws Exception {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    StackId stackId = createNiceMock(StackId.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("C1", "HIVE", "HCAT", "HCAT", "Host100",  "STARTED", "", null, null, null);
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("C1", "HIVE", "HIVE_METASTORE", "HIVE_METASTORE", "Host101", "INSTALLED", "", null, null, null);
    ServiceComponentHostResponse shr3 = new ServiceComponentHostResponse("C1", "HIVE", "HIVE_CLIENT", "HIVE_CLIENT", "Host100", "STARTED", "", null, null, null);
    ServiceComponentHostResponse shr4 = new ServiceComponentHostResponse("C1", "HIVE", "HIVE_SERVER", "HIVE_SERVER", "Host100", "STARTED", "", null, null, null);
    ServiceComponentHostResponse shr5 = new ServiceComponentHostResponse("C1", "HIVE", "MYSQL_SERVER", "MYSQL_SERVER", "Host100", "STARTED", "", null, null, null);
    ServiceComponentHostResponse shr6 = new ServiceComponentHostResponse("C1", "HIVE", "WEBHCAT_SERVER", "WEBHCAT_SERVER", "Host100", "STARTED", "", null, null, null);

    Set<ServiceComponentHostResponse> responses = new LinkedHashSet<ServiceComponentHostResponse>();
    responses.add(shr1);
    responses.add(shr2);
    responses.add(shr3);
    responses.add(shr4);
    responses.add(shr5);
    responses.add(shr6);

    // set expectations
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("C1")).andReturn(cluster).anyTimes();
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).andReturn(responses).anyTimes();
    expect(cluster.getDesiredStackVersion()).andReturn(stackId);

    expect(stackId.getStackName()).andReturn("S1").anyTimes();
    expect(stackId.getStackVersion()).andReturn("V1").anyTimes();

    expect(ambariMetaInfo.getComponent((String) anyObject(), (String) anyObject(),
            (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();

    expect(componentInfo.isMaster()).andReturn(false);
    expect(componentInfo.isMaster()).andReturn(true);
    expect(componentInfo.isMaster()).andReturn(false);
    expect(componentInfo.isMaster()).andReturn(true);
    expect(componentInfo.isMaster()).andReturn(true);
    expect(componentInfo.isMaster()).andReturn(true);

    // replay
    replay(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);

    ServiceResourceProvider.ServiceState serviceState = new ServiceResourceProvider.HiveServiceState();

    State state = serviceState.getState(managementController, "C1", "HIVE");
    Assert.assertEquals(State.INSTALLED, state);

    // verify
    verify(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);
  }

  @Test
  public void testHiveServiceState_STARTED() throws Exception {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    StackId stackId = createNiceMock(StackId.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("C1", "HIVE", "HCAT", "HCAT", "Host100",  "INSTALLED", "", null, null, null);
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("C1", "HIVE", "HIVE_METASTORE", "HIVE_METASTORE", "Host101", "STARTED", "", null, null, null);
    ServiceComponentHostResponse shr3 = new ServiceComponentHostResponse("C1", "HIVE", "HIVE_CLIENT", "HIVE_CLIENT", "Host100", "STARTED", "", null, null, null);
    ServiceComponentHostResponse shr4 = new ServiceComponentHostResponse("C1", "HIVE", "HIVE_SERVER", "HIVE_SERVER", "Host100", "STARTED", "", null, null, null);
    ServiceComponentHostResponse shr5 = new ServiceComponentHostResponse("C1", "HIVE", "MYSQL_SERVER", "MYSQL_SERVER", "Host100", "STARTED", "", null, null, null);
    ServiceComponentHostResponse shr6 = new ServiceComponentHostResponse("C1", "HIVE", "WEBHCAT_SERVER", "WEBHCAT_SERVER", "Host100", "STARTED", "", null, null, null);

    Set<ServiceComponentHostResponse> responses = new LinkedHashSet<ServiceComponentHostResponse>();
    responses.add(shr1);
    responses.add(shr2);
    responses.add(shr3);
    responses.add(shr4);
    responses.add(shr5);
    responses.add(shr6);

    // set expectations
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("C1")).andReturn(cluster).anyTimes();
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).andReturn(responses).anyTimes();
    expect(cluster.getDesiredStackVersion()).andReturn(stackId);

    expect(stackId.getStackName()).andReturn("S1").anyTimes();
    expect(stackId.getStackVersion()).andReturn("V1").anyTimes();

    expect(ambariMetaInfo.getComponent((String) anyObject(), (String) anyObject(),
            (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();

    expect(componentInfo.isMaster()).andReturn(false);
    expect(componentInfo.isMaster()).andReturn(true);
    expect(componentInfo.isMaster()).andReturn(false);
    expect(componentInfo.isMaster()).andReturn(true);
    expect(componentInfo.isMaster()).andReturn(true);
    expect(componentInfo.isMaster()).andReturn(true);

    // replay
    replay(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);

    ServiceResourceProvider.ServiceState serviceState = new ServiceResourceProvider.HiveServiceState();

    State state = serviceState.getState(managementController, "C1", "HIVE");
    Assert.assertEquals(State.STARTED, state);

    // verify
    verify(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);
  }

  @Test
  public void testHiveServiceState_STARTED_HA() throws Exception {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    StackId stackId = createNiceMock(StackId.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("C1", "HIVE", "HCAT", "HCAT", "Host100",  "STARTED", "", null, null, null);
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("C1", "HIVE", "HIVE_METASTORE", "HIVE_METASTORE", "Host101", "STARTED", "", null, null, null);
    ServiceComponentHostResponse shr3 = new ServiceComponentHostResponse("C1", "HIVE", "HIVE_METASTORE", "HIVE_METASTORE", "Host101", "INSTALLED", "", null, null, null);
    ServiceComponentHostResponse shr4 = new ServiceComponentHostResponse("C1", "HIVE", "HIVE_CLIENT", "HIVE_CLIENT", "Host100", "STARTED", "", null, null, null);
    ServiceComponentHostResponse shr5 = new ServiceComponentHostResponse("C1", "HIVE", "HIVE_SERVER", "HIVE_SERVER", "Host100", "STARTED", "", null, null, null);
    ServiceComponentHostResponse shr6 = new ServiceComponentHostResponse("C1", "HIVE", "MYSQL_SERVER", "MYSQL_SERVER", "Host100", "STARTED", "", null, null, null);
    ServiceComponentHostResponse shr7 = new ServiceComponentHostResponse("C1", "HIVE", "WEBHCAT_SERVER", "WEBHCAT_SERVER", "Host100", "STARTED", "", null, null, null);

    Set<ServiceComponentHostResponse> responses = new LinkedHashSet<ServiceComponentHostResponse>();
    responses.add(shr1);
    responses.add(shr2);
    responses.add(shr3);
    responses.add(shr4);
    responses.add(shr5);
    responses.add(shr6);
    responses.add(shr7);

    // set expectations
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("C1")).andReturn(cluster).anyTimes();
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).andReturn(responses).anyTimes();
    expect(cluster.getDesiredStackVersion()).andReturn(stackId);

    expect(stackId.getStackName()).andReturn("S1").anyTimes();
    expect(stackId.getStackVersion()).andReturn("V1").anyTimes();

    expect(ambariMetaInfo.getComponent((String) anyObject(), (String) anyObject(),
            (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();

    expect(componentInfo.isMaster()).andReturn(false);
    expect(componentInfo.isMaster()).andReturn(true);
    expect(componentInfo.isMaster()).andReturn(false);
    expect(componentInfo.isMaster()).andReturn(true);
    expect(componentInfo.isMaster()).andReturn(true);
    expect(componentInfo.isMaster()).andReturn(true);

    // replay
    replay(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);

    ServiceResourceProvider.ServiceState serviceState = new ServiceResourceProvider.HiveServiceState();

    State state = serviceState.getState(managementController, "C1", "HIVE");
    Assert.assertEquals(State.STARTED, state);

    // verify
    verify(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);
  }

  @Test
  public void testOozieServiceState_STARTED() throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    StackId stackId = createNiceMock(StackId.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("C1", "OOZIE", "OOZIE_SERVER", "OOZIE_SERVER", "Host100",  "STARTED", "", null, null, null);
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("C1", "OOZIE", "OOZIE_SERVER", "OOZIE_SERVER", "Host101", "INSTALLED", "", null, null, null);
    ServiceComponentHostResponse shr3 = new ServiceComponentHostResponse("C1", "OOZIE", "OOZIE_CLIENT", "OOZIE_CLIENT", "Host100", "STARTED", "", null, null, null);

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


    expect(ambariMetaInfo.getComponent((String) anyObject(), (String) anyObject(),
            (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();

    expect(componentInfo.isMaster()).andReturn(true);
    expect(componentInfo.isMaster()).andReturn(true);
    expect(componentInfo.isMaster()).andReturn(false);

    // replay
    replay(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);

    ServiceResourceProvider.ServiceState serviceState = new ServiceResourceProvider.OozieServiceState();

    State state = serviceState.getState(managementController, "C1", "OOZIE");
    Assert.assertEquals(State.STARTED, state);

    // verify
    verify(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);
  }

  @Test
  public void testOozieServiceState_INSTALLED() throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    StackId stackId = createNiceMock(StackId.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("C1", "OOZIE", "OOZIE_SERVER", "OOZIE_SERVER", "Host100", "INSTALLED", "", null, null, null);
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("C1", "OOZIE", "OOZIE_SERVER", "OOZIE_SERVER", "Host101", "INSTALLED", "", null, null, null);
    ServiceComponentHostResponse shr3 = new ServiceComponentHostResponse("C1", "OOZIE", "OOZIE_CLIENT", "OOZIE_CLIENT", "Host100", "STARTED", "", null, null, null);

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


    expect(ambariMetaInfo.getComponent((String) anyObject(), (String) anyObject(),
            (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();

    expect(componentInfo.isMaster()).andReturn(true);
    expect(componentInfo.isMaster()).andReturn(true);
    expect(componentInfo.isMaster()).andReturn(false);

    // replay
    replay(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);

    ServiceResourceProvider.ServiceState serviceState = new ServiceResourceProvider.OozieServiceState();

    State state = serviceState.getState(managementController, "C1", "OOZIE");
    Assert.assertEquals(State.INSTALLED, state);

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

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("C1", "HBASE", "HBASE_MASTER", "HBASE_MASTER", "Host100",  "STARTED", "", null, null, null);
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("C1", "HBASE", "HBASE_MASTER", "HBASE_MASTER", "Host101", "INSTALLED", "", null, null, null);
    ServiceComponentHostResponse shr3 = new ServiceComponentHostResponse("C1", "HBASE", "HBASE_REGIONSERVER", "HBASE_REGIONSERVER", "Host100", "STARTED", "", null, null, null);

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


    expect(ambariMetaInfo.getComponent((String) anyObject(), (String) anyObject(),
        (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();

    expect(componentInfo.isMaster()).andReturn(true);
    expect(componentInfo.isMaster()).andReturn(true);
    expect(componentInfo.isMaster()).andReturn(false);

    // replay
    replay(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);

    ServiceResourceProvider.ServiceState serviceState = new ServiceResourceProvider.HBaseServiceState();

    State state = serviceState.getState(managementController, "C1", "HBASE");
    Assert.assertEquals(State.STARTED, state);

    // verify
    verify(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);
  }

  @Test
  public void testHBaseServiceState_INSTALLED() throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    StackId stackId = createNiceMock(StackId.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("C1", "HBASE", "HBASE_MASTER", "HBASE_MASTER", "Host100",  "INSTALLED", "", null, null, null);
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("C1", "HBASE", "HBASE_REGIONSERVER", "HBASE_REGIONSERVER", "Host100", "STARTED", "", null, null, null);

    Set<ServiceComponentHostResponse> responses = new LinkedHashSet<ServiceComponentHostResponse>();
    responses.add(shr1);
    responses.add(shr2);

    // set expectations
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("C1")).andReturn(cluster).anyTimes();
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).andReturn(responses).anyTimes();
    expect(cluster.getDesiredStackVersion()).andReturn(stackId);

    expect(stackId.getStackName()).andReturn("S1").anyTimes();
    expect(stackId.getStackVersion()).andReturn("V1").anyTimes();


    expect(ambariMetaInfo.getComponent((String) anyObject(), (String) anyObject(),
        (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();

    expect(componentInfo.isMaster()).andReturn(true);
    expect(componentInfo.isMaster()).andReturn(false);

    // replay
    replay(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);

    ServiceResourceProvider.ServiceState serviceState = new ServiceResourceProvider.HBaseServiceState();

    State state = serviceState.getState(managementController, "C1", "HBASE");
    Assert.assertEquals(State.INSTALLED, state);

    // verify
    verify(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);
  }

  @Test
  public void testHBaseServiceState_INSTALLED_NO_MASTER() throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    StackId stackId = createNiceMock(StackId.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("C1", "HBASE", "HBASE_REGIONSERVER", "HBASE_REGIONSERVER", "Host100", "STARTED", "", null, null, null);

    Set<ServiceComponentHostResponse> responses = new LinkedHashSet<ServiceComponentHostResponse>();
    responses.add(shr1);

    // set expectations
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("C1")).andReturn(cluster).anyTimes();
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).andReturn(responses).anyTimes();
    expect(cluster.getDesiredStackVersion()).andReturn(stackId);

    expect(stackId.getStackName()).andReturn("S1").anyTimes();
    expect(stackId.getStackVersion()).andReturn("V1").anyTimes();


    expect(ambariMetaInfo.getComponent((String) anyObject(), (String) anyObject(),
        (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();

    expect(componentInfo.isMaster()).andReturn(false);

    // replay
    replay(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);

    ServiceResourceProvider.ServiceState serviceState = new ServiceResourceProvider.HBaseServiceState();

    State state = serviceState.getState(managementController, "C1", "HBASE");
    Assert.assertEquals(State.INSTALLED, state);

    // verify
    verify(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);
  }

  @Test
  public void testGangliaServiceState_ArbitraryComponentOrder_STARTED() throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    StackId stackId = createNiceMock(StackId.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("C1", "GANGLIA", "GANGLIA_MONITOR", "GANGLIA_MONITOR", "Host100", "STARTED", "", null, null, null);
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("C1", "GANGLIA", "GANGLIA_MONITOR", "GANGLIA_MONITOR", "Host199", "UNKNOWN", "", null, null, null);
    ServiceComponentHostResponse shr3 = new ServiceComponentHostResponse("C1", "GANGLIA", "GANGLIA_SERVER", "GANGLIA_SERVER", "Host100", "STARTED", "", null, null, null);

    Set<ServiceComponentHostResponse> responses = new LinkedHashSet<ServiceComponentHostResponse>();
    responses.add(shr1);
    responses.add(shr2);
    responses.add(shr3);

    // set expectations
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("C1")).andReturn(cluster).anyTimes();
    expect(componentInfo.isMaster()).andReturn(false).once();
    expect(componentInfo.isMaster()).andReturn(false).once();
    expect(componentInfo.isMaster()).andReturn(true).once();
    expect(componentInfo.isClient()).andReturn(false).anyTimes();
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).andReturn(responses).anyTimes();
    expect(cluster.getDesiredStackVersion()).andReturn(stackId);

    expect(stackId.getStackName()).andReturn("S1").anyTimes();
    expect(stackId.getStackVersion()).andReturn("V1").anyTimes();


    expect(ambariMetaInfo.getComponent((String) anyObject(), (String) anyObject(),
            (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();

    // replay
    replay(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);

    ServiceResourceProvider.ServiceState serviceState = new ServiceResourceProvider.DefaultServiceState();

    State state = serviceState.getState(managementController, "C1", "GANGLIA");
    Assert.assertEquals(State.STARTED, state);

    // verify
    verify(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);
  }

  @Test
  public void testDefaultServiceState_ClientOnly_INSTALLED() throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    StackId stackId = createNiceMock(StackId.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("C1", "PIG", "PIG", "PIG", "Host100", "INSTALLED", "", null, null, null);
    shr1.setMaintenanceState(MaintenanceState.OFF.toString());

    Set<ServiceComponentHostResponse> responses = new LinkedHashSet<ServiceComponentHostResponse>();
    responses.add(shr1);

    // set expectations
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("C1")).andReturn(cluster).anyTimes();
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).andReturn(responses).anyTimes();
    expect(cluster.getDesiredStackVersion()).andReturn(stackId);

    expect(stackId.getStackName()).andReturn("S1").anyTimes();
    expect(stackId.getStackVersion()).andReturn("V1").anyTimes();


    expect(ambariMetaInfo.getComponent((String) anyObject(), (String) anyObject(),
        (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();

    expect(componentInfo.isClient()).andReturn(true);

    // replay
    replay(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);

    ServiceResourceProvider.ServiceState serviceState = new ServiceResourceProvider.DefaultServiceState();

    State state = serviceState.getState(managementController, "C1", "PIG");
    Assert.assertEquals(State.INSTALLED, state);

    // verify
    verify(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);
  }

  @Test
  public void testDefaultServiceState_ClientOnly_INSTALL_FAILED() throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    StackId stackId = createNiceMock(StackId.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("C1", "PIG", "PIG", "PIG", "Host100", "INSTALL_FAILED", "", null, null, null);
    shr1.setMaintenanceState(MaintenanceState.OFF.toString());

    Set<ServiceComponentHostResponse> responses = new LinkedHashSet<ServiceComponentHostResponse>();
    responses.add(shr1);

    // set expectations
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("C1")).andReturn(cluster).anyTimes();
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).andReturn(responses).anyTimes();
    expect(cluster.getDesiredStackVersion()).andReturn(stackId);

    expect(stackId.getStackName()).andReturn("S1").anyTimes();
    expect(stackId.getStackVersion()).andReturn("V1").anyTimes();


    expect(ambariMetaInfo.getComponent((String) anyObject(), (String) anyObject(),
        (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();

    expect(componentInfo.isClient()).andReturn(true);

    // replay
    replay(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);

    ServiceResourceProvider.ServiceState serviceState = new ServiceResourceProvider.DefaultServiceState();

    State state = serviceState.getState(managementController, "C1", "PIG");
    Assert.assertEquals(State.INSTALL_FAILED, state);

    // verify
    verify(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);
  }
  
  @Test
  public void testFlumeServiceState_STARTED() throws Exception {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("C1", "FLUME", "FLUME_HANDLER", "FLUME_HANDLER", "Host100", "STARTED", "", null, null, null);
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("C1", "FLUME", "FLUME_HANDLER", "FLUME_HANDLER", "Host200", "STARTED", "", null, null, null);

    Set<ServiceComponentHostResponse> responses = new LinkedHashSet<ServiceComponentHostResponse>();
    responses.add(shr1);
    responses.add(shr2);

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("C1")).andReturn(cluster).anyTimes();
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).andReturn(responses).anyTimes();
    
    replay(managementController, clusters, cluster);
    
    ServiceResourceProvider.ServiceState serviceState = new ServiceResourceProvider.FlumeServiceState();

    State state = serviceState.getState(managementController, "C1", "FLUME");
    
    Assert.assertEquals(State.STARTED, state);
    
    verify(managementController, clusters, cluster);
  }
  
  @Test
  public void testFlumeServiceState_INSTALLED() throws Exception {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("C1", "FLUME", "FLUME_HANDLER", "FLUME_HANDLER", "Host100", "STARTED", "", null, null, null);
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("C1", "FLUME", "FLUME_HANDLER", "FLUME_HANDLER", "Host200", "INSTALLED", "", null, null, null);

    Set<ServiceComponentHostResponse> responses = new LinkedHashSet<ServiceComponentHostResponse>();
    responses.add(shr1);
    responses.add(shr2);

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("C1")).andReturn(cluster).anyTimes();
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).andReturn(responses).anyTimes();
    
    replay(managementController, clusters, cluster);
    
    ServiceResourceProvider.ServiceState serviceState = new ServiceResourceProvider.FlumeServiceState();

    State state = serviceState.getState(managementController, "C1", "FLUME");
    Assert.assertEquals(State.INSTALLED, state);
    
    verify(managementController, clusters, cluster);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testDefaultServiceState_Master_In_MM() throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    StackId stackId = createNiceMock(StackId.class);
    ComponentInfo componentInfo = createStrictMock(ComponentInfo.class);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("C1", "YARN", "RESOURCEMANAGER", "RESOURCEMANAGER", "Host100", "INSTALLED", "", null, null, null);
    shr1.setMaintenanceState(MaintenanceState.ON.toString());
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("C1", "YARN", "RESOURCEMANAGER", "RESOURCEMANAGER", "Host101", "STARTED", "", null, null, null);
    shr2.setMaintenanceState(MaintenanceState.OFF.toString());
    ServiceComponentHostResponse shr3 = new ServiceComponentHostResponse("C1", "YARN", "NODEMANAGER", "NODEMANAGER", "Host100", "STARTED", "", null, null, null);
    shr3.setMaintenanceState(MaintenanceState.OFF.toString());

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


    expect(ambariMetaInfo.getComponent((String) anyObject(), (String) anyObject(),
            (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();

    expect(componentInfo.isClient()).andReturn(false).anyTimes();
    expect(componentInfo.isMaster()).andReturn(true);

    expect(componentInfo.isClient()).andReturn(false).anyTimes();
    expect(componentInfo.isMaster()).andReturn(true);

    expect(componentInfo.isClient()).andReturn(false).anyTimes();
    expect(componentInfo.isMaster()).andReturn(false);

    expect(componentInfo.isClient()).andReturn(false);

    // replay
    replay(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);

    ServiceResourceProvider.ServiceState serviceState = new ServiceResourceProvider.DefaultServiceState();

    State state = serviceState.getState(managementController, "C1", "YARN");
    Assert.assertEquals(State.STARTED, state);

    // verify
    verify(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testDefaultServiceState_Slave_In_MM() throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    StackId stackId = createNiceMock(StackId.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("C1", "YARN", "NODEMANAGER", "NODEMANAGER", "Host100", "INSTALLED", "", null, null, null);
    shr1.setMaintenanceState(MaintenanceState.ON.toString());
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("C1", "YARN", "NODEMANAGER", "NODEMANAGER", "Host101", "STARTED", "", null, null, null);
    shr2.setMaintenanceState(MaintenanceState.OFF.toString());

    Set<ServiceComponentHostResponse> responses = new LinkedHashSet<ServiceComponentHostResponse>();
    responses.add(shr1);
    responses.add(shr2);

    // set expectations
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("C1")).andReturn(cluster).anyTimes();
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).andReturn(responses).anyTimes();
    expect(cluster.getDesiredStackVersion()).andReturn(stackId);

    expect(stackId.getStackName()).andReturn("S1").anyTimes();
    expect(stackId.getStackVersion()).andReturn("V1").anyTimes();


    expect(ambariMetaInfo.getComponent((String) anyObject(), (String) anyObject(),
            (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();

    expect(componentInfo.isMaster()).andReturn(false).anyTimes();

    expect(componentInfo.isClient()).andReturn(false).anyTimes();

    // replay
    replay(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);

    ServiceResourceProvider.ServiceState serviceState = new ServiceResourceProvider.DefaultServiceState();

    State state = serviceState.getState(managementController, "C1", "YARN");
    Assert.assertEquals(State.STARTED, state);

    // verify
    verify(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);
  }


  @Test
  @SuppressWarnings("unchecked")
  /**
   * Tests the case when all service components are in MM (so we base on MM
   * components state when calculating entire service state)
   */
  public void testDefaultServiceState_All_Components_In_MM() throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    StackId stackId = createNiceMock(StackId.class);
    ComponentInfo componentInfo = createStrictMock(ComponentInfo.class);

    /*
     * Any component is started, all other are stopped - service is considered STARTED
     */
    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("C1", "YARN", "RESOURCEMANAGER", "RESOURCEMANAGER", "Host100", "INSTALLED", "", null, null, null);
    shr1.setMaintenanceState(MaintenanceState.ON.toString());
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("C1", "YARN", "RESOURCEMANAGER", "RESOURCEMANAGER", "Host101", "INSTALLED", "", null, null, null);
    shr2.setMaintenanceState(MaintenanceState.ON.toString());
    ServiceComponentHostResponse shr3 = new ServiceComponentHostResponse("C1", "YARN", "NODEMANAGER", "NODEMANAGER", "Host100", "STARTED", "", null, null, null);
    shr3.setMaintenanceState(MaintenanceState.ON.toString());
    ServiceComponentHostResponse shr4 = new ServiceComponentHostResponse("C1", "YARN", "YARN_CLIENT", "YARN_CLIENT", "Host100", "INSTALLED", "", null, null, null);
    shr4.setMaintenanceState(MaintenanceState.ON.toString());


    Set<ServiceComponentHostResponse> responses = new LinkedHashSet<ServiceComponentHostResponse>();
    responses.add(shr1);
    responses.add(shr2);
    responses.add(shr3);
    responses.add(shr4);

    // set expectations
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("C1")).andReturn(cluster).anyTimes();
    expect(managementController.getHostComponents((Set<ServiceComponentHostRequest>) anyObject())).andReturn(responses).anyTimes();
    expect(cluster.getDesiredStackVersion()).andReturn(stackId).anyTimes();

    expect(stackId.getStackName()).andReturn("S1").anyTimes();
    expect(stackId.getStackVersion()).andReturn("V1").anyTimes();


    expect(ambariMetaInfo.getComponent((String) anyObject(), (String) anyObject(),
            (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();

    expect(componentInfo.isClient()).andReturn(false);
    expect(componentInfo.isMaster()).andReturn(true);

    expect(componentInfo.isClient()).andReturn(false);
    expect(componentInfo.isMaster()).andReturn(true);

    expect(componentInfo.isClient()).andReturn(false);
    expect(componentInfo.isMaster()).andReturn(false);
    expect(componentInfo.isClient()).andReturn(false);

    expect(componentInfo.isClient()).andReturn(true);
    expect(componentInfo.isMaster()).andReturn(false);

    expect(componentInfo.isClient()).andReturn(true).times(2);

    // replay
    replay(managementController, clusters, cluster, ambariMetaInfo, stackId, componentInfo);

    ServiceResourceProvider.ServiceState serviceState = new ServiceResourceProvider.DefaultServiceState();

    State state = serviceState.getState(managementController, "C1", "YARN");
    Assert.assertEquals(State.STARTED, state);

    /*
     * All components are stopped, service is considered INSTALLED
     */
    reset(componentInfo);

    responses.clear();
    shr1 = new ServiceComponentHostResponse("C1", "YARN", "RESOURCEMANAGER", "RESOURCEMANAGER", "Host100", "INSTALLED", "", null, null, null);
    shr1.setMaintenanceState(MaintenanceState.ON.toString());
    shr2 = new ServiceComponentHostResponse("C1", "YARN", "RESOURCEMANAGER", "RESOURCEMANAGER", "Host101", "INSTALLED", "", null, null, null);
    shr2.setMaintenanceState(MaintenanceState.ON.toString());
    shr3 = new ServiceComponentHostResponse("C1", "YARN", "NODEMANAGER", "NODEMANAGER", "Host100", "INSTALLED", "", null, null, null);
    shr3.setMaintenanceState(MaintenanceState.ON.toString());
    shr4 = new ServiceComponentHostResponse("C1", "YARN", "YARN_CLIENT", "YARN_CLIENT", "Host100", "INSTALLED", "", null, null, null);
    shr4.setMaintenanceState(MaintenanceState.ON.toString());
    responses.add(shr1);
    responses.add(shr2);
    responses.add(shr3);
    responses.add(shr4);

    expect(componentInfo.isClient()).andReturn(false);
    expect(componentInfo.isMaster()).andReturn(true);

    expect(componentInfo.isClient()).andReturn(false);
    expect(componentInfo.isMaster()).andReturn(true);

    expect(componentInfo.isClient()).andReturn(false);
    expect(componentInfo.isMaster()).andReturn(false);
    expect(componentInfo.isClient()).andReturn(false);

    expect(componentInfo.isClient()).andReturn(true);
    expect(componentInfo.isMaster()).andReturn(false);

    expect(componentInfo.isClient()).andReturn(true).times(2);

    replay(componentInfo);

    state = serviceState.getState(managementController, "C1", "YARN");
    Assert.assertEquals(State.INSTALLED, state);
  }


  /**
   * This factory method creates default MaintenanceStateHelper mock.
   * It's useful in most cases (when we don't care about Maintenance State)
   */
  public static ServiceResourceProvider getServiceProvider(AmbariManagementController managementController) throws  AmbariException {
    MaintenanceStateHelper maintenanceStateHelperMock = createNiceMock(MaintenanceStateHelper.class);
    expect(maintenanceStateHelperMock.isOperationAllowed(anyObject(Resource.Type.class), anyObject(Service.class))).andReturn(true).anyTimes();
    expect(maintenanceStateHelperMock.isOperationAllowed(anyObject(Resource.Type.class), anyObject(ServiceComponentHost.class))).andReturn(true).anyTimes();
    replay(maintenanceStateHelperMock);
    return getServiceProvider(managementController, maintenanceStateHelperMock);
  }

  /**
   * This factory method allows to define custom MaintenanceStateHelper mock.
   */
  public static ServiceResourceProvider getServiceProvider(AmbariManagementController managementController,
                                                           MaintenanceStateHelper maintenanceStateHelper) {
    Resource.Type type = Resource.Type.Service;
    return new ServiceResourceProvider(PropertyHelper.getPropertyIds(type),
            PropertyHelper.getKeyPropertyIds(type),
            managementController, maintenanceStateHelper);
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
    return updateServices(controller, requests, requestProperties, runSmokeTest, reconfigureClients, null);
  }

  /**
   * Allows to set maintenanceStateHelper. For use when there is anything to test
   * with maintenance mode.
   */
  public static RequestStatusResponse updateServices(AmbariManagementController controller,
                                                     Set<ServiceRequest> requests,
                                                     Map<String, String> requestProperties, boolean runSmokeTest,
                                                     boolean reconfigureClients,
                                                     MaintenanceStateHelper maintenanceStateHelper) throws AmbariException
  {
    ServiceResourceProvider provider;
    if (maintenanceStateHelper != null) {
      provider = getServiceProvider(controller, maintenanceStateHelper);
    } else {
      provider = getServiceProvider(controller);
    }

    RequestStageContainer request = provider.updateServices(null, requests, requestProperties, runSmokeTest, reconfigureClients, true);
    request.persist();
    return request.getRequestStatusResponse();
  }



  public static RequestStatusResponse deleteServices(AmbariManagementController controller, Set<ServiceRequest> requests)
      throws AmbariException {
    ServiceResourceProvider provider = getServiceProvider(controller);
    return provider.deleteServices(requests);
  }

}
