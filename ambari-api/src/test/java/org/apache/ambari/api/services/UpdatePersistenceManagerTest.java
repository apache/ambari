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

package org.apache.ambari.api.services;


import org.apache.ambari.api.controller.utilities.PropertyHelper;
import org.apache.ambari.api.query.Query;
import org.apache.ambari.api.resources.ResourceDefinition;
import org.apache.ambari.server.controller.spi.*;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

import static org.easymock.EasyMock.*;

/**
 * Unit tests for UpdatePersistenceManager.
 */
public class UpdatePersistenceManagerTest {
  @Test
  public void testPersist() throws Exception {
    ResourceDefinition resource = createMock(ResourceDefinition.class);
    ClusterController controller = createMock(ClusterController.class);
    Schema schema = createMock(Schema.class);
    org.apache.ambari.server.controller.spi.Request serverRequest =
        createStrictMock(org.apache.ambari.server.controller.spi.Request.class);
    Query query = createMock(Query.class);
    Predicate predicate = createMock(Predicate.class);


    Map<PropertyId, String> mapProperties = new HashMap<PropertyId, String>();
    mapProperties.put(PropertyHelper.getPropertyId("bar", "foo"), "value");

    //expectations
    expect(resource.getType()).andReturn(Resource.Type.Component);
    expect(resource.getProperties()).andReturn(mapProperties);
    expect(resource.getQuery()).andReturn(query);
    expect(query.getPredicate()).andReturn(predicate);

    controller.updateResources(Resource.Type.Component, serverRequest, predicate);

    replay(resource, controller, schema, serverRequest, query, predicate);

    new TestUpdatePersistenceManager(controller, mapProperties, serverRequest).persist(resource);

    verify(resource, controller, schema, serverRequest, query, predicate);
  }

  private class TestUpdatePersistenceManager extends UpdatePersistenceManager {

    private ClusterController m_controller;
    private org.apache.ambari.server.controller.spi.Request m_request;
    private Map<PropertyId, String> m_mapProperties;

    private TestUpdatePersistenceManager(ClusterController controller,
                                         Map<PropertyId, String> mapProperties,
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
    protected org.apache.ambari.server.controller.spi.Request createControllerRequest(Map<PropertyId, String> properties) {
      assertEquals(m_mapProperties, properties);
      return m_request;
    }
  }

}
