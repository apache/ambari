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


import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.api.query.Query;
import org.apache.ambari.server.controller.spi.*;
import org.apache.ambari.server.api.resources.ResourceDefinition;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.persistence.PersistenceManagerImpl;
import org.apache.ambari.server.controller.internal.RequestStatusImpl;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

/**
 * PersistenceManagerImpl unit tests.
 */
public class PersistenceManagerImplTest {

  @Test
  public void testCreate() throws Exception {
    ResourceInstance resource = createMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createMock(ResourceDefinition.class);
    ClusterController controller = createMock(ClusterController.class);
    Schema schema = createMock(Schema.class);
    String clusterId = "clusterId";
    String serviceId = "serviceId";
    Request serverRequest = createStrictMock(Request.class);

    Map<Resource.Type, String> mapResourceIds = new HashMap<Resource.Type, String>();
    mapResourceIds.put(Resource.Type.Cluster, "clusterId");
    mapResourceIds.put(Resource.Type.Service, "serviceId");

    Set<Map<String, Object>> setProperties = new HashSet<Map<String, Object>>();
    Map<String, Object> mapProperties = new HashMap<String, Object>();
    mapProperties.put(clusterId, "clusterId");
    mapProperties.put(serviceId, "serviceId");
    mapProperties.put(PropertyHelper.getPropertyId("foo", "bar"), "value");
    setProperties.add(mapProperties);

    //expectations
    expect(resource.getIds()).andReturn(mapResourceIds);
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition).atLeastOnce();
    expect(resourceDefinition.getType()).andReturn(Resource.Type.Component);
    expect(controller.getSchema(Resource.Type.Component)).andReturn(schema);
    expect(schema.getKeyPropertyId(Resource.Type.Cluster)).andReturn(clusterId);
    expect(schema.getKeyPropertyId(Resource.Type.Service)).andReturn(serviceId);

    expect(controller.createResources(Resource.Type.Component, serverRequest)).andReturn(new RequestStatusImpl(null));

    replay(resource, resourceDefinition, controller, schema, serverRequest);

    new TestPersistenceManager(controller, setProperties, serverRequest).create(resource, setProperties);

    verify(resource, resourceDefinition, controller, schema, serverRequest);
  }

  @Test
  public void testCreate__MultipleResources() throws Exception {
    ResourceInstance resource = createMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createMock(ResourceDefinition.class);
    ClusterController controller = createMock(ClusterController.class);
    Schema schema = createMock(Schema.class);
    Request serverRequest = createStrictMock(Request.class);

    String clusterId = "clusterId";
    String serviceId = "serviceId";


    Map<Resource.Type, String> mapResourceIds = new HashMap<Resource.Type, String>();
    mapResourceIds.put(Resource.Type.Cluster, "clusterId");
    mapResourceIds.put(Resource.Type.Service, "serviceId");

    Set<Map<String, Object>> setProperties = new HashSet<Map<String, Object>>();

    Map<String, Object> mapResourceProps1 = new HashMap<String, Object>();
    mapResourceProps1.put(clusterId, "clusterId");
    mapResourceProps1.put(serviceId, "serviceId");
    mapResourceProps1.put(PropertyHelper.getPropertyId("foo", "bar"), "value");

    Map<String, Object> mapResourceProps2 = new HashMap<String, Object>();
    mapResourceProps2.put(clusterId, "clusterId");
    mapResourceProps2.put(serviceId, "serviceId2");
    mapResourceProps2.put(PropertyHelper.getPropertyId("foo", "bar2"), "value2");

    setProperties.add(mapResourceProps1);
    setProperties.add(mapResourceProps2);

    //expectations
    expect(resource.getIds()).andReturn(mapResourceIds);
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition);
    expect(resourceDefinition.getType()).andReturn(Resource.Type.Component);
    expect(controller.getSchema(Resource.Type.Component)).andReturn(schema);
    expect(schema.getKeyPropertyId(Resource.Type.Cluster)).andReturn(clusterId).times(2);
    expect(schema.getKeyPropertyId(Resource.Type.Service)).andReturn(serviceId).times(2);

    expect(controller.createResources(Resource.Type.Component, serverRequest)).andReturn(new RequestStatusImpl(null));

    replay(resource, resourceDefinition, controller, schema, serverRequest);

    new TestPersistenceManager(controller, setProperties, serverRequest).create(resource, setProperties);

    verify(resource, resourceDefinition, controller, schema, serverRequest);
  }

  @Test
  public void testUpdate() throws Exception {
    ResourceInstance resource = createMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createMock(ResourceDefinition.class);
    ClusterController controller = createMock(ClusterController.class);
    Schema schema = createMock(Schema.class);
    Request serverRequest = createStrictMock(Request.class);
    Query query = createMock(Query.class);
    Predicate predicate = createMock(Predicate.class);

    Set<Map<String, Object>> setProperties = new HashSet<Map<String, Object>>();
    Map<String, Object> mapProperties = new HashMap<String, Object>();
    mapProperties.put(PropertyHelper.getPropertyId("foo", "bar"), "value");
    setProperties.add(mapProperties);

    //expectations
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition);
    expect(resourceDefinition.getType()).andReturn(Resource.Type.Component);
    expect(resource.getQuery()).andReturn(query);
    expect(query.getPredicate()).andReturn(predicate);

    expect(controller.updateResources(Resource.Type.Component, serverRequest, predicate)).andReturn(new RequestStatusImpl(null));

    replay(resource, resourceDefinition, controller, schema, serverRequest, query, predicate);

    new TestPersistenceManager(controller, setProperties, serverRequest).update(resource, setProperties);

    verify(resource, resourceDefinition, controller, schema, serverRequest, query, predicate);
  }

  @Test
  public void testDelete() throws Exception {
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

    new TestPersistenceManager(controller, null, null).delete(resource, null);

    verify(resource, resourceDefinition, controller, query, predicate);
  }


  private class TestPersistenceManager extends PersistenceManagerImpl {

    private Request m_request;
    private Set<Map<String, Object>> m_setProperties;

    private TestPersistenceManager(ClusterController controller,
                                         Set<Map<String, Object>> setProperties,
                                         Request controllerRequest) {
      super(controller);
      m_setProperties = setProperties;
      m_request = controllerRequest;
    }

    @Override
    protected Request createControllerRequest(Set<Map<String, Object>> setProperties) {
      assertEquals(m_setProperties, setProperties);
      return m_request;
    }
  }
}
