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


package org.apache.ambari.server.api.handlers;

import org.apache.ambari.server.api.query.Query;
import org.apache.ambari.server.api.resources.ResourceDefinition;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.resources.ResourceInstanceFactory;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.api.services.persistence.PersistenceManager;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.api.util.TreeNodeImpl;
import org.apache.ambari.server.controller.spi.*;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Test;

import java.util.*;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;


/**
 * Unit tests for QueryCreateHandler.
 */
public class QueryCreateHandlerTest {

  @Test
  public void testHandleRequest() throws Exception {
    Request request = createNiceMock(Request.class);
    ResourceInstance resourceInstance = createNiceMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);
    ResourceInstanceFactory resourceInstanceFactory = createNiceMock(ResourceInstanceFactory.class);
    Query query = createNiceMock(Query.class);
    Predicate predicate = createNiceMock(Predicate.class);
    Result result = createNiceMock(Result.class);
    ResourceInstance subResource = createNiceMock(ResourceInstance.class);
    ResourceDefinition subResourceDefinition = createNiceMock(ResourceDefinition.class);
    ClusterController controller = createNiceMock(ClusterController.class);
    Schema serviceSchema = createNiceMock(Schema.class);
    Schema componentSchema = createNiceMock(Schema.class);
    String resourceKeyProperty = "resourceKeyProperty";
    String createKeyProperty = "createKeyProperty";
    Resource resource1 = createNiceMock(Resource.class);
    Resource resource2 = createNiceMock(Resource.class);
    PersistenceManager pm = createNiceMock(PersistenceManager.class);
    ResourceInstance createResource = createNiceMock(ResourceInstance.class);
    RequestStatus status = createNiceMock(RequestStatus.class);
    Resource statusResource1 = createNiceMock(Resource.class);
    Resource statusResource2 = createNiceMock(Resource.class);
    RequestHandler readHandler = createStrictMock(RequestHandler.class);
    ResultStatus resultStatus = createNiceMock(ResultStatus.class);

    String httpBody = "{" +
        "\"components\" : [" +
        "{\"ServiceComponentInfo\" : {" +
        "        \"component_name\" : \"SECONDARY_NAMENODE\"" +
        "      }" +
        "}," +
        "{\"ServiceComponentInfo\" : {" +
        "        \"component_name\" : \"HDFS_CLIENT\"" +
        "      }" +
        "}" +
        "] }";

    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();

    Set<Map<String, Object>> setRequestProps = new HashSet<Map<String, Object>>();
    setRequestProps.add(Collections.<String, Object>singletonMap(
        PropertyHelper.getPropertyId("ServiceComponentInfo", "component_name"), "SECONDARY_NAMENODE"));
    setRequestProps.add(Collections.<String, Object>singletonMap(
        PropertyHelper.getPropertyId("ServiceComponentInfo", "component_name"), "HDFS_CLIENT"));

    Set<Map<String, Object>> setCreateProps = new HashSet<Map<String, Object>>();
    Map<String, Object> map1 = new HashMap<String, Object>();
    map1.put(PropertyHelper.getPropertyId("ServiceComponentInfo", "component_name"), "SECONDARY_NAMENODE");
    map1.put(createKeyProperty, "id1");
    setCreateProps.add(map1);
    Map<String, Object> map2 = new HashMap<String, Object>();
    map2.put(PropertyHelper.getPropertyId("ServiceComponentInfo", "component_name"), "SECONDARY_NAMENODE");
    map2.put(createKeyProperty, "id2");
    setCreateProps.add(map2);
    Map<String, Object> map3 = new HashMap<String, Object>();
    map3.put(PropertyHelper.getPropertyId("ServiceComponentInfo", "component_name"), "HDFS_CLIENT");
    map3.put(createKeyProperty, "id1");
    setCreateProps.add(map3);
    Map<String, Object> map4 = new HashMap<String, Object>();
    map4.put(PropertyHelper.getPropertyId("ServiceComponentInfo", "component_name"), "HDFS_CLIENT");
    map4.put(createKeyProperty, "id2");
    setCreateProps.add(map4);

    Map<String, ResourceInstance> mapSubResources = new HashMap<String, ResourceInstance>();
    mapSubResources.put("components", subResource);

    TreeNode<Resource> resultTree = new TreeNodeImpl<Resource>(null, null, "result");
    resultTree.addChild(resource1, "resource1");
    resultTree.addChild(resource2, "resource2");

    Set<Resource> setStatusResources = new HashSet<Resource>();
    setStatusResources.add(statusResource1);
    setStatusResources.add(statusResource2);

    //expectations
    expect(readHandler.handleRequest(request)).andReturn(result);
    expect(result.getStatus()).andReturn(resultStatus).anyTimes();
    expect(resultStatus.isErrorState()).andReturn(false);
    expect(result.getResultTree()).andReturn(resultTree);

    expect(request.getResource()).andReturn(resourceInstance).anyTimes();
    expect(request.getHttpBody()).andReturn(httpBody).anyTimes();
    expect(request.getHttpBodyProperties()).andReturn(setRequestProps).anyTimes();

    expect(resourceInstance.getResourceDefinition()).andReturn(resourceDefinition).anyTimes();
    expect(resourceInstance.getIds()).andReturn(mapIds).anyTimes();
    expect(resourceInstance.getSubResources()).andReturn(mapSubResources).anyTimes();

    expect(resourceDefinition.getType()).andReturn(Resource.Type.Service).anyTimes();

    expect(subResource.getResourceDefinition()).andReturn(subResourceDefinition).anyTimes();
    expect(subResourceDefinition.getType()).andReturn(Resource.Type.Component).anyTimes();

    expect(controller.getSchema(Resource.Type.Service)).andReturn(serviceSchema).anyTimes();
    expect(controller.getSchema(Resource.Type.Component)).andReturn(componentSchema).anyTimes();

    expect(serviceSchema.getKeyPropertyId(Resource.Type.Service)).andReturn(resourceKeyProperty).anyTimes();
    expect(componentSchema.getKeyPropertyId(Resource.Type.Service)).andReturn(createKeyProperty).anyTimes();

    expect(result.getResultTree()).andReturn(resultTree).anyTimes();
    expect(resource1.getPropertyValue(resourceKeyProperty)).andReturn("id1").anyTimes();
    expect(resource2.getPropertyValue(resourceKeyProperty)).andReturn("id2").anyTimes();

    expect(resourceInstanceFactory.createResource(Resource.Type.Component, mapIds)).
        andReturn(createResource).anyTimes();

    expect(pm.create(createResource, setCreateProps)).andReturn(status);
    expect(status.getStatus()).andReturn(RequestStatus.Status.Complete).anyTimes();
    expect(status.getAssociatedResources()).andReturn(setStatusResources).anyTimes();

    expect(statusResource1.getType()).andReturn(Resource.Type.Component).anyTimes();
    expect(statusResource2.getType()).andReturn(Resource.Type.Component).anyTimes();

    replay(request, resourceInstance, resourceDefinition, query, predicate, result, subResource,
        subResourceDefinition, controller, serviceSchema, componentSchema, resource1, resource2,
        pm, resourceInstanceFactory, createResource, status, statusResource1, statusResource2,
        readHandler, resultStatus);

    //test
    Result testResult = new TestQueryCreateHandler(resourceInstanceFactory, controller, pm, readHandler).
        handleRequest(request);

    Collection<TreeNode<Resource>> children = testResult.getResultTree().getChild("resources").getChildren();
    assertEquals(2, children.size());
    boolean containsStatusResource1 = false;
    boolean containsStatusResource2 = false;
    for (TreeNode<Resource> child : children) {
      Resource r = child.getObject();
      if (r == statusResource1) {
        containsStatusResource1 = true;
      } else if(r == statusResource2) {
        containsStatusResource2 = true;
      }
    }
    assertTrue(containsStatusResource1);
    assertTrue(containsStatusResource2);
    assertEquals(ResultStatus.STATUS.CREATED, testResult.getStatus().getStatus());

    verify(request, resourceInstance, resourceDefinition, query, predicate, result, subResource,
        subResourceDefinition, controller, serviceSchema, componentSchema, resource1, resource2,
        pm, resourceInstanceFactory, createResource, status, statusResource1, statusResource2,
        readHandler, resultStatus);
  }

  static class TestQueryCreateHandler extends QueryCreateHandler {
    private ResourceInstanceFactory m_resourceFactory;
    private ClusterController m_controller;
    private PersistenceManager m_testPm;
    private RequestHandler m_testReadHandler;

    TestQueryCreateHandler(ResourceInstanceFactory resourceFactory, ClusterController controller,
                           PersistenceManager pm, RequestHandler readHandler) {
      m_resourceFactory = resourceFactory;
      m_controller = controller;
      m_testPm = pm;
      m_testReadHandler = readHandler;
    }

    @Override
    protected ResourceInstanceFactory getResourceFactory() {
      return m_resourceFactory;
    }

    @Override
    protected ClusterController getClusterController() {
      return m_controller;
    }

    @Override
    protected PersistenceManager getPersistenceManager() {
      return m_testPm;
    }

    @Override
    protected RequestHandler getReadHandler() {
      return m_testReadHandler;
    }
  }

}
