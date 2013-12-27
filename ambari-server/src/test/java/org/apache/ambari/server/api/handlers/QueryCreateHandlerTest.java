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
import org.apache.ambari.server.api.services.*;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.persistence.PersistenceManager;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.api.util.TreeNodeImpl;
import org.apache.ambari.server.controller.spi.*;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.easymock.*;
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
    final String BODY_STRING = "Body string";
    Request request = createNiceMock(Request.class);
    RequestBody body = createNiceMock(RequestBody.class);
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
    Capture<RequestBody> bodyCapture = new Capture<RequestBody>();

    //  test request body
    //    {
    //      "components" : [
    //        { "ServiceComponentInfo" : {
    //            "component_name" : "SECONDARY_NAMENODE"
    //          }
    //        },
    //        { "ServiceComponentInfo" : {
    //            "component_name" : "HDFS_CLIENT"
    //          }
    //        }
    //      ]
    //   }

    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();

    Set<NamedPropertySet> setRequestProps = new HashSet<NamedPropertySet>();

    Map<String, Object> mapProperties = new HashMap<String, Object>();
    Set<Map<String, Object>> arraySet  = new HashSet<Map<String, Object>>();

    mapProperties.put("components", arraySet);

    Map<String, Object> map = new HashMap<String, Object>();
    map.put(PropertyHelper.getPropertyId("ServiceComponentInfo", "component_name"), "SECONDARY_NAMENODE");
    arraySet.add(map);

    map = new HashMap<String, Object>();
    map.put(PropertyHelper.getPropertyId("ServiceComponentInfo", "component_name"), "HDFS_CLIENT");
    arraySet.add(map);

    setRequestProps.add(new NamedPropertySet("", mapProperties));


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

    expect(body.getBody()).andReturn(BODY_STRING).anyTimes();
    
    expect(request.getResource()).andReturn(resourceInstance).anyTimes();
    expect(request.getBody()).andReturn(body).anyTimes();
    expect(body.getNamedPropertySets()).andReturn(setRequestProps).anyTimes();

    expect(resourceInstance.getResourceDefinition()).andReturn(resourceDefinition).anyTimes();
    expect(resourceInstance.getKeyValueMap()).andReturn(mapIds).anyTimes();
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

    expect(pm.create(same(createResource), capture(bodyCapture))).andReturn(status);
    expect(status.getStatus()).andReturn(RequestStatus.Status.Complete).anyTimes();
    expect(status.getAssociatedResources()).andReturn(setStatusResources).anyTimes();

    expect(statusResource1.getType()).andReturn(Resource.Type.Component).anyTimes();
    expect(statusResource2.getType()).andReturn(Resource.Type.Component).anyTimes();

    replay(request, body, resourceInstance, resourceDefinition, query, predicate, result, subResource,
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

    RequestBody createBody = bodyCapture.getValue();
    assertEquals(BODY_STRING, createBody.getBody());        
    assertEquals(4, createBody.getPropertySets().size());
    assertEquals(setCreateProps, createBody.getPropertySets());
    verify(request, body, resourceInstance, resourceDefinition, query, predicate, result, subResource,
        subResourceDefinition, controller, serviceSchema, componentSchema, resource1, resource2,
        pm, resourceInstanceFactory, createResource, status, statusResource1, statusResource2,
        readHandler, resultStatus);
  }

  @Test
  public void tesHandleRequest_NoSubResourceNameSpecified() {
    Request request = createNiceMock(Request.class);
    RequestBody body = createNiceMock(RequestBody.class);
    ResourceInstance resourceInstance = createNiceMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);
    Query query = createNiceMock(Query.class);
    Predicate predicate = createNiceMock(Predicate.class);
    Result result = createNiceMock(Result.class);
    ResourceInstance subResource = createNiceMock(ResourceInstance.class);
    ResourceDefinition subResourceDefinition = createNiceMock(ResourceDefinition.class);
    ClusterController controller = createNiceMock(ClusterController.class);
    Schema serviceSchema = createNiceMock(Schema.class);
    Schema componentSchema = createNiceMock(Schema.class);
    String resourceKeyProperty = "resourceKeyProperty";
    Resource resource1 = createNiceMock(Resource.class);
    Resource resource2 = createNiceMock(Resource.class);
    PersistenceManager pm = createNiceMock(PersistenceManager.class);
    ResourceInstance createResource = createNiceMock(ResourceInstance.class);
    RequestStatus status = createNiceMock(RequestStatus.class);
    Resource statusResource1 = createNiceMock(Resource.class);
    Resource statusResource2 = createNiceMock(Resource.class);
    RequestHandler readHandler = createStrictMock(RequestHandler.class);
    ResultStatus queryResultStatus = createNiceMock(ResultStatus.class);

    //  test request body.  Missing sub-resource name.
    //    {
    //      { "ServiceComponentInfo" : {
    //          "component_name" : "SECONDARY_NAMENODE"
    //        }
    //      },
    //      { "ServiceComponentInfo" : {
    //          "component_name" : "HDFS_CLIENT"
    //        }
    //      }
    //   }

    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();

    Set<NamedPropertySet> setRequestProps = new HashSet<NamedPropertySet>();
    Map<String, Object> mapProperties = new HashMap<String, Object>();
    Set<Map<String, Object>> arraySet  = new HashSet<Map<String, Object>>();

    mapProperties.put("", arraySet);

    Map<String, Object> map = new HashMap<String, Object>();
    map.put(PropertyHelper.getPropertyId("ServiceComponentInfo", "component_name"), "SECONDARY_NAMENODE");
    arraySet.add(map);

    map = new HashMap<String, Object>();
    map.put(PropertyHelper.getPropertyId("ServiceComponentInfo", "component_name"), "HDFS_CLIENT");
    arraySet.add(map);

    setRequestProps.add(new NamedPropertySet("", mapProperties));

    TreeNode<Resource> resultTree = new TreeNodeImpl<Resource>(null, null, "result");
    resultTree.addChild(resource1, "resource1");
    resultTree.addChild(resource2, "resource2");

    //expectations
    expect(readHandler.handleRequest(request)).andReturn(result);
    expect(result.getStatus()).andReturn(queryResultStatus).anyTimes();
    expect(queryResultStatus.isErrorState()).andReturn(false);
    expect(result.getResultTree()).andReturn(resultTree);

    expect(request.getResource()).andReturn(resourceInstance).anyTimes();
    expect(request.getBody()).andReturn(body).anyTimes();
    expect(body.getNamedPropertySets()).andReturn(setRequestProps).anyTimes();

    expect(resourceInstance.getResourceDefinition()).andReturn(resourceDefinition).anyTimes();
    expect(resourceInstance.getKeyValueMap()).andReturn(mapIds).anyTimes();

    expect(resourceDefinition.getType()).andReturn(Resource.Type.Service).anyTimes();
    expect(controller.getSchema(Resource.Type.Service)).andReturn(serviceSchema).anyTimes();
    expect(result.getResultTree()).andReturn(resultTree).anyTimes();
    expect(resource1.getPropertyValue(resourceKeyProperty)).andReturn("id1").anyTimes();
    expect(resource2.getPropertyValue(resourceKeyProperty)).andReturn("id2").anyTimes();

    replay(request, body, resourceInstance, resourceDefinition, query, predicate, result, subResource,
        subResourceDefinition, controller, serviceSchema, componentSchema, resource1, resource2,
        pm, createResource, status, statusResource1, statusResource2,
        readHandler, queryResultStatus);

    //test
    Result testResult = new TestQueryCreateHandler(null, controller, pm, readHandler).
        handleRequest(request);

    ResultStatus resultStatus = testResult.getStatus();
    assertEquals(ResultStatus.STATUS.BAD_REQUEST, resultStatus.getStatus());
    assertEquals("Invalid Request: A sub-resource name must be supplied.", resultStatus.getMessage());

    verify(request, body, resourceInstance, resourceDefinition, query, predicate, result, subResource,
        subResourceDefinition, controller, serviceSchema, componentSchema, resource1, resource2,
        pm, createResource, status, statusResource1, statusResource2,
        readHandler, queryResultStatus);

  }

  @Test
  public void tesHandleRequest_InvalidSubResSpecified() {
    Request request = createNiceMock(Request.class);
    RequestBody body = createNiceMock(RequestBody.class);
    ResourceInstance resourceInstance = createNiceMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);
    Query query = createNiceMock(Query.class);
    Predicate predicate = createNiceMock(Predicate.class);
    Result result = createNiceMock(Result.class);
    ResourceInstance subResource = createNiceMock(ResourceInstance.class);
    ResourceDefinition subResourceDefinition = createNiceMock(ResourceDefinition.class);
    ClusterController controller = createNiceMock(ClusterController.class);
    Schema serviceSchema = createNiceMock(Schema.class);
    Schema componentSchema = createNiceMock(Schema.class);
    String resourceKeyProperty = "resourceKeyProperty";
    Resource resource1 = createNiceMock(Resource.class);
    Resource resource2 = createNiceMock(Resource.class);
    PersistenceManager pm = createNiceMock(PersistenceManager.class);
    ResourceInstance createResource = createNiceMock(ResourceInstance.class);
    RequestStatus status = createNiceMock(RequestStatus.class);
    Resource statusResource1 = createNiceMock(Resource.class);
    Resource statusResource2 = createNiceMock(Resource.class);
    RequestHandler readHandler = createStrictMock(RequestHandler.class);
    ResultStatus queryResultStatus = createNiceMock(ResultStatus.class);

    //  test request body
    //    {
    //      "INVALID" : [
    //        { "ServiceComponentInfo" : {
    //            "component_name" : "SECONDARY_NAMENODE"
    //          }
    //        },
    //        { "ServiceComponentInfo" : {
    //            "component_name" : "HDFS_CLIENT"
    //          }
    //        }
    //      ]
    //   }

    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();

    Set<NamedPropertySet> setRequestProps = new HashSet<NamedPropertySet>();
    Map<String, Object> mapProperties = new HashMap<String, Object>();
    Set<Map<String, Object>> arraySet  = new HashSet<Map<String, Object>>();

    mapProperties.put("INVALID", arraySet);

    Map<String, Object> map = new HashMap<String, Object>();
    map.put(PropertyHelper.getPropertyId("ServiceComponentInfo", "component_name"), "SECONDARY_NAMENODE");
    arraySet.add(map);

    map = new HashMap<String, Object>();
    map.put(PropertyHelper.getPropertyId("ServiceComponentInfo", "component_name"), "HDFS_CLIENT");
    arraySet.add(map);

    setRequestProps.add(new NamedPropertySet("", mapProperties));

    Map<String, ResourceInstance> mapSubResources = new HashMap<String, ResourceInstance>();
    mapSubResources.put("components", subResource);

    TreeNode<Resource> resultTree = new TreeNodeImpl<Resource>(null, null, "result");
    resultTree.addChild(resource1, "resource1");
    resultTree.addChild(resource2, "resource2");

    //expectations
    expect(readHandler.handleRequest(request)).andReturn(result);
    expect(result.getStatus()).andReturn(queryResultStatus).anyTimes();
    expect(queryResultStatus.isErrorState()).andReturn(false);
    expect(result.getResultTree()).andReturn(resultTree);

    expect(request.getResource()).andReturn(resourceInstance).anyTimes();
    expect(request.getBody()).andReturn(body).anyTimes();
    expect(body.getNamedPropertySets()).andReturn(setRequestProps).anyTimes();

    expect(resourceInstance.getResourceDefinition()).andReturn(resourceDefinition).anyTimes();
    expect(resourceInstance.getKeyValueMap()).andReturn(mapIds).anyTimes();
    expect(resourceInstance.getSubResources()).andReturn(mapSubResources).anyTimes();

    expect(resourceDefinition.getType()).andReturn(Resource.Type.Service).anyTimes();
    expect(controller.getSchema(Resource.Type.Service)).andReturn(serviceSchema).anyTimes();
    expect(result.getResultTree()).andReturn(resultTree).anyTimes();
    expect(resource1.getPropertyValue(resourceKeyProperty)).andReturn("id1").anyTimes();
    expect(resource2.getPropertyValue(resourceKeyProperty)).andReturn("id2").anyTimes();

    replay(request, body, resourceInstance, resourceDefinition, query, predicate, result, subResource,
        subResourceDefinition, controller, serviceSchema, componentSchema, resource1, resource2,
        pm, createResource, status, statusResource1, statusResource2,
        readHandler, queryResultStatus);

    //test
    Result testResult = new TestQueryCreateHandler(null, controller, pm, readHandler).
        handleRequest(request);

    ResultStatus resultStatus = testResult.getStatus();
    assertEquals(ResultStatus.STATUS.BAD_REQUEST, resultStatus.getStatus());
    assertEquals("Invalid Request: The specified sub-resource name is not valid: 'INVALID'.", resultStatus.getMessage());

    verify(request, body, resourceInstance, resourceDefinition, query, predicate, result, subResource,
        subResourceDefinition, controller, serviceSchema, componentSchema, resource1, resource2,
        pm, createResource, status, statusResource1, statusResource2,
        readHandler, queryResultStatus);

  }

  @Test
  public void tesHandleRequest_NoSubResourcesSpecified() {
    Request request = createNiceMock(Request.class);
    RequestBody body = createNiceMock(RequestBody.class);
    ResourceInstance resourceInstance = createNiceMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);
    Query query = createNiceMock(Query.class);
    Predicate predicate = createNiceMock(Predicate.class);
    Result result = createNiceMock(Result.class);
    ResourceInstance subResource = createNiceMock(ResourceInstance.class);
    ResourceDefinition subResourceDefinition = createNiceMock(ResourceDefinition.class);
    ClusterController controller = createNiceMock(ClusterController.class);
    Schema serviceSchema = createNiceMock(Schema.class);
    Schema componentSchema = createNiceMock(Schema.class);
    String resourceKeyProperty = "resourceKeyProperty";
    Resource resource1 = createNiceMock(Resource.class);
    Resource resource2 = createNiceMock(Resource.class);
    PersistenceManager pm = createNiceMock(PersistenceManager.class);
    ResourceInstance createResource = createNiceMock(ResourceInstance.class);
    RequestStatus status = createNiceMock(RequestStatus.class);
    Resource statusResource1 = createNiceMock(Resource.class);
    Resource statusResource2 = createNiceMock(Resource.class);
    RequestHandler readHandler = createStrictMock(RequestHandler.class);
    ResultStatus queryResultStatus = createNiceMock(ResultStatus.class);

    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();

    Set<NamedPropertySet> setRequestProps = new HashSet<NamedPropertySet>();
    // no body specified so no props

    TreeNode<Resource> resultTree = new TreeNodeImpl<Resource>(null, null, "result");
    resultTree.addChild(resource1, "resource1");
    resultTree.addChild(resource2, "resource2");

    //expectations
    expect(readHandler.handleRequest(request)).andReturn(result);
    expect(result.getStatus()).andReturn(queryResultStatus).anyTimes();
    expect(queryResultStatus.isErrorState()).andReturn(false);
    expect(result.getResultTree()).andReturn(resultTree);

    expect(request.getResource()).andReturn(resourceInstance).anyTimes();
    expect(request.getBody()).andReturn(body).anyTimes();
    expect(body.getNamedPropertySets()).andReturn(setRequestProps).anyTimes();

    expect(resourceInstance.getResourceDefinition()).andReturn(resourceDefinition).anyTimes();
    expect(resourceInstance.getKeyValueMap()).andReturn(mapIds).anyTimes();

    expect(resourceDefinition.getType()).andReturn(Resource.Type.Service).anyTimes();
    expect(controller.getSchema(Resource.Type.Service)).andReturn(serviceSchema).anyTimes();
    expect(result.getResultTree()).andReturn(resultTree).anyTimes();
    expect(resource1.getPropertyValue(resourceKeyProperty)).andReturn("id1").anyTimes();
    expect(resource2.getPropertyValue(resourceKeyProperty)).andReturn("id2").anyTimes();

    replay(request, body, resourceInstance, resourceDefinition, query, predicate, result, subResource,
        subResourceDefinition, controller, serviceSchema, componentSchema, resource1, resource2,
        pm, createResource, status, statusResource1, statusResource2,
        readHandler, queryResultStatus);

    //test
    Result testResult = new TestQueryCreateHandler(null, controller, pm, readHandler).
        handleRequest(request);

    ResultStatus resultStatus = testResult.getStatus();
    assertEquals(ResultStatus.STATUS.BAD_REQUEST, resultStatus.getStatus());
    assertEquals("Invalid Request: A minimum of one sub-resource must be specified for creation.", resultStatus.getMessage());

    verify(request, body, resourceInstance, resourceDefinition, query, predicate, result, subResource,
        subResourceDefinition, controller, serviceSchema, componentSchema, resource1, resource2,
        pm, createResource, status, statusResource1, statusResource2,
        readHandler, queryResultStatus);

  }

  //todo: this is currently not supported.  We may wish to add this support in the future.
  @Test
  public void testHandleRequest_MultipleSubResources() throws Exception {
    Request request = createNiceMock(Request.class);
    RequestBody body = createNiceMock(RequestBody.class);
    ResourceInstance resourceInstance = createNiceMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);
    ResourceInstanceFactory resourceInstanceFactory = createNiceMock(ResourceInstanceFactory.class);
    Query query = createNiceMock(Query.class);
    Predicate predicate = createNiceMock(Predicate.class);
    Result result = createNiceMock(Result.class);
    ResourceInstance subResource1 = createNiceMock(ResourceInstance.class);
    ResourceInstance subResource2 = createNiceMock(ResourceInstance.class);
    ResourceDefinition subResourceDefinition1 = createNiceMock(ResourceDefinition.class);
    ResourceDefinition subResourceDefinition2 = createNiceMock(ResourceDefinition.class);
    ClusterController controller = createNiceMock(ClusterController.class);
    Schema serviceSchema = createNiceMock(Schema.class);
    Schema subResSchema1 = createNiceMock(Schema.class);
    Schema subResSchema2 = createNiceMock(Schema.class);
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
    ResultStatus queryResultStatus = createNiceMock(ResultStatus.class);

    //  test request body.  Multiple valid sub-resource types
    //  {
    //    "foo" : [
    //      { "prop" : "val" }
    //    ],
    //    "bar" : [
    //      { "prop" : "val" }
    //    ]
    //  }

    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();

    Set<NamedPropertySet> setRequestProps = new HashSet<NamedPropertySet>();
    Map<String, Object>   mapProperties   = new HashMap<String, Object>();

    Set<Map<String, Object>> arraySet = new HashSet<Map<String, Object>>();
    mapProperties.put("foo", arraySet);

    Map<String, Object> map = new HashMap<String, Object>();
    map.put("prop", "val");
    arraySet.add(map);

    arraySet = new HashSet<Map<String, Object>>();
    mapProperties.put("bar", arraySet);

    map = new HashMap<String, Object>();
    map.put("prop", "val");
    arraySet.add(map);

    setRequestProps.add(new NamedPropertySet("", mapProperties));

    Map<String, ResourceInstance> mapSubResources = new HashMap<String, ResourceInstance>();
    mapSubResources.put("foo", subResource1);
    mapSubResources.put("bar", subResource2);

    TreeNode<Resource> resultTree = new TreeNodeImpl<Resource>(null, null, "result");
    resultTree.addChild(resource1, "resource1");
    resultTree.addChild(resource2, "resource2");

    //expectations
    expect(readHandler.handleRequest(request)).andReturn(result);
    expect(result.getStatus()).andReturn(queryResultStatus).anyTimes();
    expect(queryResultStatus.isErrorState()).andReturn(false);
    expect(result.getResultTree()).andReturn(resultTree);

    expect(request.getResource()).andReturn(resourceInstance).anyTimes();
    expect(request.getBody()).andReturn(body).anyTimes();
    expect(body.getNamedPropertySets()).andReturn(setRequestProps).anyTimes();

    expect(resourceInstance.getResourceDefinition()).andReturn(resourceDefinition).anyTimes();
    expect(resourceInstance.getKeyValueMap()).andReturn(mapIds).anyTimes();
    expect(resourceInstance.getSubResources()).andReturn(mapSubResources).anyTimes();

    expect(resourceDefinition.getType()).andReturn(Resource.Type.Service).anyTimes();

    expect(subResource1.getResourceDefinition()).andReturn(subResourceDefinition1).anyTimes();
    expect(subResourceDefinition1.getType()).andReturn(Resource.Type.Component).anyTimes();
    expect(subResource2.getResourceDefinition()).andReturn(subResourceDefinition2).anyTimes();
    expect(subResourceDefinition2.getType()).andReturn(Resource.Type.HostComponent).anyTimes();

    expect(controller.getSchema(Resource.Type.Service)).andReturn(serviceSchema).anyTimes();
    expect(controller.getSchema(Resource.Type.Component)).andReturn(subResSchema1).anyTimes();
    expect(controller.getSchema(Resource.Type.HostComponent)).andReturn(subResSchema2).anyTimes();

    expect(serviceSchema.getKeyPropertyId(Resource.Type.Service)).andReturn(resourceKeyProperty).anyTimes();
    expect(subResSchema1.getKeyPropertyId(Resource.Type.Service)).andReturn(createKeyProperty).anyTimes();
    expect(subResSchema2.getKeyPropertyId(Resource.Type.Service)).andReturn(createKeyProperty).anyTimes();

    expect(result.getResultTree()).andReturn(resultTree).anyTimes();
    expect(resource1.getPropertyValue(resourceKeyProperty)).andReturn("id1").anyTimes();
    expect(resource2.getPropertyValue(resourceKeyProperty)).andReturn("id2").anyTimes();


    replay(request, body, resourceInstance, resourceDefinition, query, predicate, result, subResource1, subResource2,
        subResourceDefinition1, subResourceDefinition2, controller, serviceSchema, subResSchema1, subResSchema2,
        resource1, resource2, pm, resourceInstanceFactory, createResource, status, statusResource1, statusResource2,
        readHandler, queryResultStatus);

    //test
    Result testResult = new TestQueryCreateHandler(resourceInstanceFactory, controller, pm, readHandler).
        handleRequest(request);

    ResultStatus resultStatus = testResult.getStatus();
    assertEquals(ResultStatus.STATUS.BAD_REQUEST, resultStatus.getStatus());
    assertEquals("Invalid Request: Multiple sub-resource types may not be created in the same request.",
        resultStatus.getMessage());


    verify(request, body, resourceInstance, resourceDefinition, query, predicate, result, subResource1, subResource2,
        subResourceDefinition1, subResourceDefinition2, controller, serviceSchema, subResSchema1, subResSchema2,
        resource1, resource2, pm, resourceInstanceFactory, createResource, status, statusResource1, statusResource2,
        readHandler, queryResultStatus);
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
