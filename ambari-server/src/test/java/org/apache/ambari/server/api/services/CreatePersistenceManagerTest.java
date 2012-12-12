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

package org.apache.ambari.server.api.services;

import org.apache.ambari.server.api.resources.ResourceDefinition;
import org.apache.ambari.server.controller.internal.RequestStatusImpl;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Schema;
import org.apache.ambari.server.controller.spi.Request;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

import static org.easymock.EasyMock.*;

/**
 * Unit tests for CreatePersistenceManager.
 */
public class CreatePersistenceManagerTest {
  @Test
  public void testPersist() throws Exception {
    ResourceDefinition resource = createMock(ResourceDefinition.class);
    ClusterController controller = createMock(ClusterController.class);
    Schema schema = createMock(Schema.class);
    PropertyId clusterId = createStrictMock(PropertyId.class);
    PropertyId serviceId = createStrictMock(PropertyId.class);
    Request serverRequest = createStrictMock(Request.class);

    Map<Resource.Type, String> mapResourceIds = new HashMap<Resource.Type, String>();
    mapResourceIds.put(Resource.Type.Cluster, "clusterId");
    mapResourceIds.put(Resource.Type.Service, "serviceId");

    Set<Map<PropertyId, Object>> setProperties = new HashSet<Map<PropertyId, Object>>();
    Map<PropertyId, Object> mapProperties = new HashMap<PropertyId, Object>();
    mapProperties.put(clusterId, "clusterId");
    mapProperties.put(serviceId, "serviceId");
    mapProperties.put(PropertyHelper.getPropertyId("bar", "foo"), "value");
    setProperties.add(mapProperties);

    //expectations
    expect(resource.getResourceIds()).andReturn(mapResourceIds);
    expect(resource.getType()).andReturn(Resource.Type.Component);
    expect(controller.getSchema(Resource.Type.Component)).andReturn(schema);
    expect(schema.getKeyPropertyId(Resource.Type.Cluster)).andReturn(clusterId);
    expect(schema.getKeyPropertyId(Resource.Type.Service)).andReturn(serviceId);

    expect(controller.createResources(Resource.Type.Component, serverRequest)).andReturn(new RequestStatusImpl(null));

    replay(resource, controller, schema, clusterId, serviceId, serverRequest);

    new TestCreatePersistenceManager(controller, setProperties, serverRequest).persist(resource, setProperties);

    verify(resource, controller, schema, clusterId, serviceId, serverRequest);

  }

  @Test
  public void testPersist__MultipleResources() throws Exception {
    ResourceDefinition resource = createMock(ResourceDefinition.class);
    ClusterController controller = createMock(ClusterController.class);
    Schema schema = createMock(Schema.class);
    PropertyId clusterId = createStrictMock(PropertyId.class);
    PropertyId serviceId = createStrictMock(PropertyId.class);
    Request serverRequest = createStrictMock(Request.class);

    Map<Resource.Type, String> mapResourceIds = new HashMap<Resource.Type, String>();
    mapResourceIds.put(Resource.Type.Cluster, "clusterId");
    mapResourceIds.put(Resource.Type.Service, "serviceId");

    Set<Map<PropertyId, Object>> setProperties = new HashSet<Map<PropertyId, Object>>();

    Map<PropertyId, Object> mapResourceProps1 = new HashMap<PropertyId, Object>();
    mapResourceProps1.put(clusterId, "clusterId");
    mapResourceProps1.put(serviceId, "serviceId");
    mapResourceProps1.put(PropertyHelper.getPropertyId("bar", "foo"), "value");

    Map<PropertyId, Object> mapResourceProps2 = new HashMap<PropertyId, Object>();
    mapResourceProps2.put(clusterId, "clusterId");
    mapResourceProps2.put(serviceId, "serviceId2");
    mapResourceProps2.put(PropertyHelper.getPropertyId("bar2", "foo"), "value2");

    setProperties.add(mapResourceProps1);
    setProperties.add(mapResourceProps2);

    //expectations
    expect(resource.getResourceIds()).andReturn(mapResourceIds);
    expect(resource.getType()).andReturn(Resource.Type.Component);
    expect(controller.getSchema(Resource.Type.Component)).andReturn(schema);
    expect(schema.getKeyPropertyId(Resource.Type.Cluster)).andReturn(clusterId).times(2);
    expect(schema.getKeyPropertyId(Resource.Type.Service)).andReturn(serviceId).times(2);

    expect(controller.createResources(Resource.Type.Component, serverRequest)).andReturn(new RequestStatusImpl(null));

    replay(resource, controller, schema, clusterId, serviceId, serverRequest);

    new TestCreatePersistenceManager(controller, setProperties, serverRequest).persist(resource, setProperties);

    verify(resource, controller, schema, clusterId, serviceId, serverRequest);

  }

  private class TestCreatePersistenceManager extends CreatePersistenceManager {

    private ClusterController m_controller;
    private Request m_request;
    private Set<Map<PropertyId, Object>> m_setProperties;

    private TestCreatePersistenceManager(ClusterController controller,
                                         Set<Map<PropertyId, Object>> setProperties,
                                         Request controllerRequest) {
      m_controller = controller;
      m_setProperties = setProperties;
      m_request = controllerRequest;
    }

    @Override
    protected ClusterController getClusterController() {
      return m_controller;
    }

    @Override
    protected Request createControllerRequest(Set<Map<PropertyId, Object>> setProperties) {
      assertEquals(m_setProperties, setProperties);
      return m_request;
    }
  }

}
