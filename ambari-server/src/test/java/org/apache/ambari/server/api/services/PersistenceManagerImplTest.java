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
    RequestBody body = new RequestBody();

    Map<Resource.Type, String> mapResourceIds = new HashMap<Resource.Type, String>();
    mapResourceIds.put(Resource.Type.Cluster, "clusterId");
    mapResourceIds.put(Resource.Type.Service, "serviceId");

    Map<String, Object> mapProperties = new HashMap<String, Object>();
    mapProperties.put("componentId", "id");
    mapProperties.put(PropertyHelper.getPropertyId("foo", "bar"), "value");
    NamedPropertySet namedPropSet = new NamedPropertySet("", mapProperties);
    body.addPropertySet(namedPropSet);

    Set<Map<String, Object>> setExpected = new HashSet<Map<String, Object>>();
    Map<String, Object> mapExpected = new HashMap<String, Object>(mapProperties);
    mapExpected.put(clusterId, "clusterId");
    mapExpected.put(serviceId, "serviceId");
    setExpected.add(mapExpected);

    //expectations
    expect(resource.getKeyValueMap()).andReturn(mapResourceIds);
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition).atLeastOnce();
    expect(resourceDefinition.getType()).andReturn(Resource.Type.Component);
    expect(controller.getSchema(Resource.Type.Component)).andReturn(schema);
    expect(schema.getKeyPropertyId(Resource.Type.Cluster)).andReturn(clusterId);
    expect(schema.getKeyPropertyId(Resource.Type.Service)).andReturn(serviceId);

    expect(controller.createResources(Resource.Type.Component, serverRequest)).andReturn(new RequestStatusImpl(null));

    replay(resource, resourceDefinition, controller, schema, serverRequest);

    new TestPersistenceManager(controller, setExpected, serverRequest).create(resource, body);

    verify(resource, resourceDefinition, controller, schema, serverRequest);
  }

  @Test
  public void testCreate___NoBodyProps() throws Exception {
    ResourceInstance resource = createMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createMock(ResourceDefinition.class);
    ClusterController controller = createMock(ClusterController.class);
    Schema schema = createMock(Schema.class);
    String clusterId = "clusterId";
    String serviceId = "serviceId";
    String componentId = "componentId";
    Request serverRequest = createStrictMock(Request.class);
    RequestBody body = new RequestBody();

    Map<Resource.Type, String> mapResourceIds = new HashMap<Resource.Type, String>();
    mapResourceIds.put(Resource.Type.Cluster, "clusterId");
    mapResourceIds.put(Resource.Type.Service, "serviceId");
    mapResourceIds.put(Resource.Type.Component, "componentId");

    Set<Map<String, Object>> setExpected = new HashSet<Map<String, Object>>();
    Map<String, Object> mapExpected = new HashMap<String, Object>();
    mapExpected.put(clusterId, "clusterId");
    mapExpected.put(serviceId, "serviceId");
    mapExpected.put(componentId, "componentId");
    setExpected.add(mapExpected);

    //expectations
    expect(resource.getKeyValueMap()).andReturn(mapResourceIds);
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition).atLeastOnce();
    expect(resourceDefinition.getType()).andReturn(Resource.Type.Component);
    expect(controller.getSchema(Resource.Type.Component)).andReturn(schema);
    expect(schema.getKeyPropertyId(Resource.Type.Cluster)).andReturn(clusterId);
    expect(schema.getKeyPropertyId(Resource.Type.Service)).andReturn(serviceId);
    expect(schema.getKeyPropertyId(Resource.Type.Component)).andReturn(componentId);

    expect(controller.createResources(Resource.Type.Component, serverRequest)).andReturn(new RequestStatusImpl(null));

    replay(resource, resourceDefinition, controller, schema, serverRequest);

    new TestPersistenceManager(controller, setExpected, serverRequest).create(resource, body);

    verify(resource, resourceDefinition, controller, schema, serverRequest);
  }

  @Test
  public void testCreate__MultipleResources() throws Exception {
    ResourceInstance resource = createMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createMock(ResourceDefinition.class);
    ClusterController controller = createMock(ClusterController.class);
    Schema schema = createMock(Schema.class);
    Request serverRequest = createStrictMock(Request.class);
    RequestBody body = new RequestBody();

    String clusterId = "clusterId";
    String serviceId = "serviceId";

    Map<Resource.Type, String> mapResourceIds = new HashMap<Resource.Type, String>();
    mapResourceIds.put(Resource.Type.Cluster, "clusterId");
    mapResourceIds.put(Resource.Type.Service, "serviceId");

    Map<String, Object> mapResourceProps1 = new HashMap<String, Object>();
    mapResourceProps1.put("componentId", "id1");
    mapResourceProps1.put(PropertyHelper.getPropertyId("foo", "bar"), "value");

    Map<String, Object> mapResourceProps2 = new HashMap<String, Object>();
    mapResourceProps1.put("componentId", "id2");
    mapResourceProps2.put(PropertyHelper.getPropertyId("foo", "bar2"), "value2");

    NamedPropertySet namedPropSet1 = new NamedPropertySet("", mapResourceProps1);
    NamedPropertySet namedPropSet2 = new NamedPropertySet("", mapResourceProps2);

    body.addPropertySet(namedPropSet1);
    body.addPropertySet(namedPropSet2);

    Set<Map<String, Object>> setExpected = new HashSet<Map<String, Object>>();
    Map<String, Object> mapExpected1 = new HashMap<String, Object>(mapResourceProps1);
    mapExpected1.put(clusterId, "clusterId");
    mapExpected1.put(serviceId, "serviceId");
    setExpected.add(mapExpected1);
    Map<String, Object> mapExpected2 = new HashMap<String, Object>(mapResourceProps2);
    mapExpected2.put(clusterId, "clusterId");
    mapExpected2.put(serviceId, "serviceId");
    setExpected.add(mapExpected2);

    //expectations
    expect(resource.getKeyValueMap()).andReturn(mapResourceIds);
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition);
    expect(resourceDefinition.getType()).andReturn(Resource.Type.Component);
    expect(controller.getSchema(Resource.Type.Component)).andReturn(schema);
    expect(schema.getKeyPropertyId(Resource.Type.Cluster)).andReturn(clusterId).times(2);
    expect(schema.getKeyPropertyId(Resource.Type.Service)).andReturn(serviceId).times(2);

    expect(controller.createResources(Resource.Type.Component, serverRequest)).andReturn(new RequestStatusImpl(null));

    replay(resource, resourceDefinition, controller, schema, serverRequest);

    new TestPersistenceManager(controller, setExpected, serverRequest).create(resource, body);

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
    RequestBody body = new RequestBody();

    Map<String, Object> mapProperties = new HashMap<String, Object>();
    mapProperties.put(PropertyHelper.getPropertyId("foo", "bar"), "value");
    NamedPropertySet namedPropSet = new NamedPropertySet("", mapProperties);
    body.addPropertySet(namedPropSet);

    Set<Map<String, Object>> setExpected = new HashSet<Map<String, Object>>();
    setExpected.add(mapProperties);

    //expectations
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition);
    expect(resourceDefinition.getType()).andReturn(Resource.Type.Component);
    expect(resource.getQuery()).andReturn(query);
    expect(query.getPredicate()).andReturn(predicate);

    expect(controller.updateResources(Resource.Type.Component, serverRequest, predicate)).andReturn(new RequestStatusImpl(null));

    replay(resource, resourceDefinition, controller, schema, serverRequest, query, predicate);

    new TestPersistenceManager(controller, setExpected, serverRequest).update(resource, body);

    verify(resource, resourceDefinition, controller, schema, serverRequest, query, predicate);
  }

  @Test
  public void testDelete() throws Exception {
    ResourceInstance resource = createNiceMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);
    ClusterController controller = createMock(ClusterController.class);
    Query query = createMock(Query.class);
    Predicate predicate = createMock(Predicate.class);
    RequestBody body = new RequestBody();

    //expectations
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition).anyTimes();
    expect(resourceDefinition.getType()).andReturn(Resource.Type.Component).anyTimes();
    expect(resource.getQuery()).andReturn(query).anyTimes();
    expect(query.getPredicate()).andReturn(predicate).anyTimes();

    expect(controller.deleteResources(Resource.Type.Component, predicate)).andReturn(new RequestStatusImpl(null));

    replay(resource, resourceDefinition, controller, query, predicate);

    new TestPersistenceManager(controller, null, null).delete(resource, body);

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
    protected Request createControllerRequest(RequestBody body) {
      assertEquals(m_setProperties, body.getPropertySets());
      //todo: assert request props
      return m_request;
    }
  }
}
