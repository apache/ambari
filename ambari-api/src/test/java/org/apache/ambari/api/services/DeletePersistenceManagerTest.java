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


import org.apache.ambari.api.query.Query;
import org.apache.ambari.api.resources.ResourceDefinition;
import org.apache.ambari.server.controller.spi.*;
import org.junit.Test;

import static org.easymock.EasyMock.*;

/**
 * Unit tests for UpdatePersistenceManager.
 */
public class DeletePersistenceManagerTest {
  @Test
  public void testPersist() throws Exception {
    ResourceDefinition resource = createMock(ResourceDefinition.class);
    ClusterController controller = createMock(ClusterController.class);
    Schema schema = createMock(Schema.class);
    Query query = createMock(Query.class);
    Predicate predicate = createMock(Predicate.class);


    //expectations
    expect(resource.getType()).andReturn(Resource.Type.Component);
    expect(resource.getQuery()).andReturn(query);
    expect(query.getPredicate()).andReturn(predicate);

    controller.deleteResources(Resource.Type.Component, predicate);

    replay(resource, controller, schema, query, predicate);

    new TestDeletePersistenceManager(controller).persist(resource);

    verify(resource, controller, schema, query, predicate);
  }


  private class TestDeletePersistenceManager extends DeletePersistenceManager {

    private ClusterController m_controller;

    private TestDeletePersistenceManager(ClusterController controller) {
      m_controller = controller;
    }

    @Override
    protected ClusterController getClusterController() {
      return m_controller;
    }
  }

}
