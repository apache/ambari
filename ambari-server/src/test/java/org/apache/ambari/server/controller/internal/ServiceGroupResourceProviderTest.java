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
package org.apache.ambari.server.controller.internal;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.resources.ResourceInstanceFactory;
import org.apache.ambari.server.api.resources.ResourceInstanceFactoryImpl;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.NamedPropertySet;
import org.apache.ambari.server.api.services.RequestBody;
import org.apache.ambari.server.api.services.parsers.JsonRequestBodyParser;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ServiceGroupRequest;
import org.apache.ambari.server.controller.ServiceGroupResponse;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Schema;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelperInitializer;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ServiceGroup;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class ServiceGroupResourceProviderTest {

  public static ServiceGroupResourceProvider getProvider(AmbariManagementController controller) {
    return new ServiceGroupResourceProvider(controller);
  }

  public static void createServiceGroups(AmbariManagementController controller, Set<ServiceGroupRequest> requests)
      throws AmbariException, AuthorizationException {
    getProvider(controller).createServiceGroups(requests);
  }

  public static void createServiceGroup(AmbariManagementController controller, String clusterName, String serviceGroupName)
      throws AmbariException, AuthorizationException {
    ServiceGroupRequest request = new ServiceGroupRequest(clusterName, serviceGroupName, "dummy-stack-name");
    createServiceGroups(controller, Collections.singleton(request));
  }

  @Before
  public void before() {
    Authentication authentication = TestAuthenticationFactory.createClusterAdministrator();
    AuthorizationHelperInitializer.viewInstanceDAOReturningNull();
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  @After
  public void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  public void testCreateServiceGroupsWithStackId() throws Exception {
    String body = "[{\"ServiceGroupInfo\":{\"service_group_name\": \"CORE\",\"stack_id\": \"HDP-1.2.3\"}}]";
    String clusterName = "c1";
    JsonRequestBodyParser jsonParser = new JsonRequestBodyParser();
    RequestBody requestBody = jsonParser.parse(body).iterator().next();
    //RequestFactory requestFactory = new RequestFactory();
    ResourceInstanceFactory resourceFactory = new ResourceInstanceFactoryImpl();
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, clusterName);
    mapIds.put(Resource.Type.ServiceGroup, null);
    ResourceInstance resource = resourceFactory.createResource(Resource.Type.ServiceGroup, mapIds);
    Map<Resource.Type, String> mapResourceIds = resource.getKeyValueMap();
    mapResourceIds.put(Resource.Type.Cluster, "c1");
    AmbariManagementController ambariManagementController = createMock(AmbariManagementController.class);
    AmbariMetaInfo ambariMetaInfo = createMock(AmbariMetaInfo.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Schema serviceGroupSchema = createNiceMock("ServiceGroupSchema", Schema.class);
    ClusterController clusterController = createNiceMock(ClusterController.class);
    ServiceGroup coreServiceGroup = createNiceMock(ServiceGroup.class);
    ServiceGroup edmServiceGroup = createNiceMock(ServiceGroup.class);
    ServiceGroupResponse coreServiceGroupResponse = new ServiceGroupResponse(1l, "c1", 1l, "CORE", "HDP-1.2.3");
    expect(ambariManagementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(ambariManagementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster(clusterName)).andReturn(cluster).anyTimes();
    expect(cluster.addServiceGroup("CORE", "HDP-1.2.3")).andReturn(coreServiceGroup).anyTimes();
    expect(coreServiceGroup.convertToResponse()).andReturn(coreServiceGroupResponse).anyTimes();
    expect(clusterController.getSchema(Resource.Type.ServiceGroup)).andReturn(serviceGroupSchema).anyTimes();
    expect(serviceGroupSchema.getKeyPropertyId(Resource.Type.Cluster)).andReturn("ServiceGroupInfo/cluster_name").anyTimes();
    expect(serviceGroupSchema.getKeyPropertyId(Resource.Type.ServiceGroup)).andReturn("ServiceGroupInfo/service_group_name").anyTimes();
    expect(serviceGroupSchema.getKeyTypes()).andReturn(Collections.singleton(Resource.Type.ServiceGroup)).anyTimes();

    replay(cluster, clusters, ambariManagementController, ambariMetaInfo, serviceGroupSchema, clusterController, coreServiceGroup, edmServiceGroup);

    ServiceGroupResourceProvider serviceGroupResourceProvider = new ServiceGroupResourceProvider(ambariManagementController);
    Set<NamedPropertySet> setProperties = requestBody.getNamedPropertySets();
    if (setProperties.isEmpty()) {
      requestBody.addPropertySet(new NamedPropertySet("", new HashMap<>()));
    }
    for (NamedPropertySet propertySet : setProperties) {
      for (Map.Entry<Resource.Type, String> entry : mapResourceIds.entrySet()) {
        Map<String, Object> mapProperties = propertySet.getProperties();
        String property = serviceGroupSchema.getKeyPropertyId(entry.getKey());
        if (!mapProperties.containsKey(property)) {
          mapProperties.put(property, entry.getValue());
        }
      }
    }

    RequestStatus requestStatus = serviceGroupResourceProvider.createResources(PropertyHelper.getCreateRequest(requestBody.getPropertySets(), requestBody.getRequestInfoProperties()));
    Assert.assertEquals(requestStatus.getStatus(), RequestStatus.Status.Complete);

    verify(cluster, clusters, ambariManagementController, ambariMetaInfo, serviceGroupSchema, clusterController, coreServiceGroup, edmServiceGroup);
  }

}
