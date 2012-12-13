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


import org.apache.ambari.server.api.query.Query;
import org.apache.ambari.server.api.resources.ResourceDefinition;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.internal.RequestStatusImpl;
import org.apache.ambari.server.controller.spi.*;
import org.junit.Test;

import static org.easymock.EasyMock.*;

/**
 * Unit tests for UpdatePersistenceManager.
 */
public class DeletePersistenceManagerTest {
  @Test
  public void testPersist() throws Exception {
    ResourceInstance resource = createNiceMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);
    ClusterController controller = createMock(ClusterController.class);
    Query query = createMock(Query.class);
    Predicate predicate = createMock(Predicate.class);

    //expectations
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition).anyTimes();
    expect(resourceDefinition.getType()).andReturn(Resource.Type.Component).anyTimes();
    expect(resource.getQuery()).andReturn(query).anyTimes();
    expect(query.getPredicate()).andReturn(predicate).anyTimes();

    expect(controller.deleteResources(Resource.Type.Component, predicate)).andReturn(new RequestStatusImpl(null));

    replay(resource, resourceDefinition, controller, query, predicate);

    new TestDeletePersistenceManager(controller).persist(resource, null);

    verify(resource, resourceDefinition, controller, query, predicate);
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
