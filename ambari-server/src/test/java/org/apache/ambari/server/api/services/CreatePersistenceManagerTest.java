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
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Schema;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

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
    org.apache.ambari.server.controller.spi.Request serverRequest =
        createStrictMock(org.apache.ambari.server.controller.spi.Request.class);

    Map<Resource.Type, String> mapResourceIds = new HashMap<Resource.Type, String>();
    mapResourceIds.put(Resource.Type.Cluster, "clusterId");
    mapResourceIds.put(Resource.Type.Service, "serviceId");

    Map<PropertyId, Object> mapProperties = new HashMap<PropertyId, Object>();
    mapProperties.put(clusterId, "clusterId");
    mapProperties.put(serviceId, "serviceId");
    mapProperties.put(PropertyHelper.getPropertyId("bar", "foo"), "value");

    //expectations
    expect(resource.getResourceIds()).andReturn(mapResourceIds);
    expect(resource.getType()).andReturn(Resource.Type.Component);
    expect(controller.getSchema(Resource.Type.Component)).andReturn(schema);
    expect(schema.getKeyPropertyId(Resource.Type.Cluster)).andReturn(clusterId);
    resource.setProperty(clusterId, "clusterId");
    expect(schema.getKeyPropertyId(Resource.Type.Service)).andReturn(serviceId);
    resource.setProperty(serviceId, "serviceId");
    expect(resource.getProperties()).andReturn(mapProperties);

    controller.createResources(Resource.Type.Component, serverRequest);

    replay(resource, controller, schema, clusterId, serviceId, serverRequest);

    new TestCreatePersistenceManager(controller, mapProperties, serverRequest).persist(resource);

    verify(resource, controller, schema, clusterId, serviceId, serverRequest);

  }

  private class TestCreatePersistenceManager extends CreatePersistenceManager {

    private ClusterController m_controller;
    private org.apache.ambari.server.controller.spi.Request m_request;
    private Map<PropertyId, Object> m_mapProperties;

    private TestCreatePersistenceManager(ClusterController controller,
                                         Map<PropertyId, Object> mapProperties,
                                         org.apache.ambari.server.controller.spi.Request controllerRequest) {
      m_controller = controller;
      m_mapProperties = mapProperties;
      m_request = controllerRequest;
    }

    @Override
    protected ClusterController getClusterController() {
      return m_controller;
    }

    @Override
    protected org.apache.ambari.server.controller.spi.Request createControllerRequest(Map<PropertyId, Object> properties) {
      assertEquals(m_mapProperties, properties);
      return m_request;
    }
  }

}
