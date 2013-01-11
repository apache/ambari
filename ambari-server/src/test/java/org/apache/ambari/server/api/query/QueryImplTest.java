package org.apache.ambari.server.api.query;

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

import org.apache.ambari.server.api.resources.ResourceDefinition;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.api.util.TreeNodeImpl;
import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.predicate.BasePredicate;
import org.apache.ambari.server.controller.spi.*;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.After;
import org.junit.Test;

import java.util.*;

import static org.easymock.EasyMock.*;

import static org.easymock.EasyMock.eq;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;


//todo: add assertions for temporal info
public class QueryImplTest {

  ClusterController m_controller = createNiceMock(ClusterController.class);

  @Test
  public void testExecute__Component_instance_noSpecifiedProps() throws Exception {
    Result result = createNiceMock(Result.class);
    ResourceInstance componentResourceInstance = createNiceMock(ResourceInstance.class);
    ResourceDefinition componentResourceDefinition = createNiceMock(ResourceDefinition.class);
    ResourceInstance hostResourceInstance = createNiceMock(ResourceInstance.class);
    ResourceDefinition hostResourceDefinition = createNiceMock(ResourceDefinition.class);
    Schema componentSchema = createNiceMock(Schema.class);
    Resource componentResource = createNiceMock(Resource.class);
    String componentPropertyId = "componentId";
    Query hostComponentQuery = createStrictMock(Query.class);
    Result hostComponentQueryResult = createNiceMock(Result.class);

    TreeNode<Resource> tree = new TreeNodeImpl<Resource>(null, null, null);
    TreeNode<Resource> hostComponentResultNode = new TreeNodeImpl<Resource>(null, null, null);
    List<Resource> listResources = Collections.singletonList(componentResource);

    Map<Resource.Type, String> mapResourceIds = new HashMap<Resource.Type, String>();
    mapResourceIds.put(Resource.Type.Cluster, "clusterName");
    mapResourceIds.put(Resource.Type.Service, "serviceName");
    mapResourceIds.put(Resource.Type.Component, "componentName");

    Map<String, ResourceInstance> mapChildren = new HashMap<String, ResourceInstance>();
    mapChildren.put("host_components", hostResourceInstance);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate = pb.property("clusterId").equals("clusterName").and().
        property("serviceId").equals("serviceName").and().
        property("componentId").equals("componentName").toPredicate();

    // expectations
    expect(componentResourceInstance.getResourceDefinition()).andReturn(componentResourceDefinition).anyTimes();
    expect(componentResourceInstance.getSubResources()).andReturn(mapChildren).anyTimes();
    expect(componentResourceInstance.getIds()).andReturn(mapResourceIds).anyTimes();

    expect(componentResourceDefinition.getType()).andReturn(Resource.Type.Component).anyTimes();

    expect(componentResource.getType()).andReturn(Resource.Type.Component).anyTimes();
    expect(componentResource.getPropertyValue(componentPropertyId)).andReturn("keyVal");

    expect(m_controller.getSchema(Resource.Type.Component)).andReturn(componentSchema).anyTimes();

    expect(componentSchema.getCategoryProperties()).andReturn(Collections.<String, Set<String>>emptyMap()).anyTimes();
    expect(componentSchema.getKeyPropertyId(Resource.Type.Cluster)).andReturn("clusterId");
    expect(componentSchema.getKeyPropertyId(Resource.Type.Service)).andReturn("serviceId");
    expect(componentSchema.getKeyPropertyId(Resource.Type.Component)).andReturn(componentPropertyId).atLeastOnce();

    expect(m_controller.getResources(eq(Resource.Type.Component), eq(PropertyHelper.getReadRequest(Collections.<String>emptySet())),
        eq(predicate))).andReturn(listResources);

    expect(result.getResultTree()).andReturn(tree).anyTimes();

    Map<Resource.Type, String> mapResourceIdsSet = new HashMap<Resource.Type, String>(mapResourceIds);
    mapResourceIdsSet.put(Resource.Type.Component, "keyVal");
    hostResourceInstance.setIds(mapResourceIdsSet);
    expect(hostResourceInstance.getResourceDefinition()).andReturn(hostResourceDefinition).anyTimes();
    expect(hostResourceInstance.getQuery()).andReturn(hostComponentQuery).anyTimes();

    expect(hostResourceDefinition.getType()).andReturn(Resource.Type.Host);
    expect(hostComponentQuery.execute()).andReturn(hostComponentQueryResult);
    expect(hostComponentQueryResult.getResultTree()).andReturn(hostComponentResultNode);

    replay(m_controller, result, componentResourceInstance, componentResourceDefinition, hostResourceInstance, componentSchema, componentResource,
        hostComponentQuery, hostComponentQueryResult);

    QueryImpl query = new TestQuery(componentResourceInstance, result);
    query.execute();

    verify(m_controller, result, componentResourceInstance, componentResourceDefinition, hostResourceInstance, componentSchema, componentResource,
        hostComponentQuery, hostComponentQueryResult);

    assertEquals(1, tree.getChildren().size());
    TreeNode<Resource> componentNode = tree.getChild("Component:1");
    assertEquals("Component:1", componentNode.getName());
    assertEquals(componentResource, componentNode.getObject());
    assertEquals(1, componentNode.getChildren().size());
    assertSame(hostComponentResultNode, componentNode.getChild("host_components"));
    assertEquals("false", hostComponentResultNode.getProperty("isCollection"));
  }

  @Test
  public void testExecute__Component_collection_noSpecifiedProps() throws Exception {
    Result result = createNiceMock(Result.class);
    ResourceInstance componentResourceInstance = createNiceMock(ResourceInstance.class);
    ResourceDefinition componentResourceDefinition = createNiceMock(ResourceDefinition.class);
    Schema componentSchema = createNiceMock(Schema.class);
    Resource componentResource = createNiceMock(Resource.class);
    String componentPropertyId = "componentId";
    String servicePropertyId = "serviceId";
    String clusterPropertyId = "clusterId";

    Set<String> setPropertyIds = new HashSet<String>();
    setPropertyIds.add(clusterPropertyId);
    setPropertyIds.add(servicePropertyId);
    setPropertyIds.add(componentPropertyId);

    Map<String, Set<String>> mapProperties = new HashMap<String, Set<String>>();
    Set<String> setAllProps = new HashSet<String>();
    setAllProps.add("clusterId");
    setAllProps.add("serviceId");
    setAllProps.add("componentId");
    mapProperties.put(null, setAllProps);

    TreeNode<Resource> tree = new TreeNodeImpl<Resource>(null, null, null);
    List<Resource> listResources = Collections.singletonList(componentResource);

    Map<Resource.Type, String> mapResourceIds = new HashMap<Resource.Type, String>();
    mapResourceIds.put(Resource.Type.Cluster, "clusterName");
    mapResourceIds.put(Resource.Type.Service, "serviceName");
    mapResourceIds.put(Resource.Type.Component, null);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate = pb.property("clusterId").equals("clusterName").and().
        property("serviceId").equals("serviceName").toPredicate();

    // expectations
    expect(componentResource.getType()).andReturn(Resource.Type.Component).anyTimes();

    expect(componentResourceInstance.getIds()).andReturn(mapResourceIds).anyTimes();
    expect(componentResourceInstance.getResourceDefinition()).andReturn(componentResourceDefinition).anyTimes();

    expect(componentResourceDefinition.getType()).andReturn(Resource.Type.Component).anyTimes();

    expect(m_controller.getSchema(Resource.Type.Component)).andReturn(componentSchema).anyTimes();

    expect(componentSchema.getCategoryProperties()).andReturn(mapProperties).anyTimes();
    expect(componentSchema.getKeyPropertyId(Resource.Type.Component)).andReturn(componentPropertyId).anyTimes();
    expect(componentSchema.getKeyPropertyId(Resource.Type.Cluster)).andReturn("clusterId").anyTimes();
    expect(componentSchema.getKeyPropertyId(Resource.Type.Service)).andReturn("serviceId").anyTimes();

    expect(result.getResultTree()).andReturn(tree).anyTimes();

    expect(m_controller.getResources(eq(Resource.Type.Component), eq(PropertyHelper.getReadRequest(setPropertyIds)),
        eq(predicate))).andReturn(listResources);

    replay(m_controller, result,componentResourceInstance, componentResourceDefinition, componentSchema, componentResource);

    QueryImpl query = new TestQuery(componentResourceInstance, result);
    query.execute();

    verify(m_controller, result, componentResourceInstance, componentResourceDefinition, componentSchema, componentResource);

    assertEquals("true", tree.getProperty("isCollection"));
    assertEquals(1, tree.getChildren().size());
    TreeNode<Resource> componentNode = tree.getChild("Component:1");
    assertSame(componentResource, componentNode.getObject());
    assertEquals(0, componentNode.getChildren().size());
  }

  @Test
  public void testExecute__collection_nullInternalPredicate_nullUserPredicate() throws Exception {
    Result result = createNiceMock(Result.class);
    ResourceInstance clusterResourceInstance = createNiceMock(ResourceInstance.class);
    ResourceDefinition clusterResourceDefinition = createNiceMock(ResourceDefinition.class);
    Schema clusterSchema = createNiceMock(Schema.class);
    Resource clusterResource = createNiceMock(Resource.class);
    String clusterPropertyId = "clusterId";

    Map<String, Set<String>> mapProperties = new HashMap<String, Set<String>>();
    mapProperties.put(null, Collections.singleton("clusterId"));

    TreeNode<Resource> tree = new TreeNodeImpl<Resource>(null, null, null);
    List<Resource> listResources = Collections.singletonList(clusterResource);

    Map<Resource.Type, String> mapResourceIds = new HashMap<Resource.Type, String>();

    // expectations
    expect(clusterResource.getType()).andReturn(Resource.Type.Cluster).anyTimes();

    expect(clusterResourceInstance.getIds()).andReturn(mapResourceIds).anyTimes();
    expect(clusterResourceInstance.getResourceDefinition()).andReturn(clusterResourceDefinition).anyTimes();

    expect(clusterResourceDefinition.getType()).andReturn(Resource.Type.Component).anyTimes();

    expect(m_controller.getSchema(Resource.Type.Component)).andReturn(clusterSchema).atLeastOnce();

    expect(clusterSchema.getCategoryProperties()).andReturn(mapProperties);
    expect(clusterSchema.getKeyPropertyId(Resource.Type.Component)).andReturn(clusterPropertyId).atLeastOnce();

    expect(result.getResultTree()).andReturn(tree).atLeastOnce();

    expect(m_controller.getResources(eq(Resource.Type.Component), eq(PropertyHelper.getReadRequest(Collections.singleton(clusterPropertyId))),
        (Predicate) isNull())).andReturn(listResources);


    replay(m_controller, result, clusterResourceInstance, clusterResourceDefinition, clusterSchema, clusterResource);

    QueryImpl query = new TestQuery(clusterResourceInstance, result);
    query.execute();

    verify(m_controller, result, clusterResourceInstance, clusterResourceDefinition, clusterSchema, clusterResource);

    assertEquals("true", tree.getProperty("isCollection"));
    assertEquals(1, tree.getChildren().size());
    TreeNode<Resource> clusterNode = tree.getChild("Cluster:1");
    assertSame(clusterResource, clusterNode.getObject());
    assertEquals(0, clusterNode.getChildren().size());

  }

  @Test
  public void testExecute__collection_nullInternalPredicate_nonNullUserPredicate() throws Exception {
    Result result = createNiceMock(Result.class);
    ResourceInstance clusterResourceInstance = createNiceMock(ResourceInstance.class);
    ResourceDefinition clusterResourceDefinition = createNiceMock(ResourceDefinition.class);
    Schema clusterSchema = createNiceMock(Schema.class);
    Resource clusterResource = createNiceMock(Resource.class);
    String clusterPropertyId = "clusterId";
    Predicate userPredicate = createNiceMock(Predicate.class);

    Map<String, Set<String>> mapProperties = new HashMap<String, Set<String>>();
    mapProperties.put(null, Collections.singleton("clusterId"));

    TreeNode<Resource> tree = new TreeNodeImpl<Resource>(null, null, null);
    List<Resource> listResources = Collections.singletonList(clusterResource);

    Map<Resource.Type, String> mapResourceIds = new HashMap<Resource.Type, String>();

    // expectations
    expect(clusterResource.getType()).andReturn(Resource.Type.Cluster).anyTimes();

    expect(clusterResourceInstance.getIds()).andReturn(mapResourceIds).anyTimes();
    expect(clusterResourceInstance.getResourceDefinition()).andReturn(clusterResourceDefinition).anyTimes();

    expect(clusterResourceDefinition.getType()).andReturn(Resource.Type.Component).atLeastOnce();

    expect(m_controller.getSchema(Resource.Type.Component)).andReturn(clusterSchema).anyTimes();
    expect(clusterSchema.getCategoryProperties()).andReturn(mapProperties).anyTimes();
    expect(clusterSchema.getKeyPropertyId(Resource.Type.Component)).andReturn(clusterPropertyId).anyTimes();

    expect(result.getResultTree()).andReturn(tree).anyTimes();

    expect(m_controller.getResources(eq(Resource.Type.Component), eq(PropertyHelper.getReadRequest(Collections.singleton(clusterPropertyId))),
        eq(userPredicate))).andReturn(listResources);

    replay(m_controller, result,clusterResourceInstance, clusterResourceDefinition, clusterSchema, clusterResource, userPredicate);

    QueryImpl query = new TestQuery(clusterResourceInstance, result);
    query.setUserPredicate(userPredicate);
    query.execute();

    verify(m_controller, result, clusterResourceInstance, clusterResourceDefinition, clusterSchema, clusterResource, userPredicate);

    assertEquals("true", tree.getProperty("isCollection"));
    assertEquals(1, tree.getChildren().size());
    TreeNode<Resource> clusterNode = tree.getChild("Cluster:1");
    assertSame(clusterResource, clusterNode.getObject());
    assertEquals(0, clusterNode.getChildren().size());
  }

  @Test
  public void testExecute__collection_nonNullInternalPredicate_nonNullUserPredicate() throws Exception {
    Result result = createNiceMock(Result.class);
    ResourceInstance componentResourceInstance = createNiceMock(ResourceInstance.class);
    ResourceDefinition componentResourceDefinition = createNiceMock(ResourceDefinition.class);
    Schema componentSchema = createNiceMock(Schema.class);
    Resource componentResource = createNiceMock(Resource.class);
    String componentPropertyId = "componentId";
    String servicePropertyId = "serviceId";
    String clusterPropertyId = "clusterId";

    Set<String> setPropertyIds = new HashSet<String>();
    setPropertyIds.add(clusterPropertyId);
    setPropertyIds.add(servicePropertyId);
    setPropertyIds.add(componentPropertyId);

    Map<String, Set<String>> mapProperties = new HashMap<String, Set<String>>();
    Set<String> setAllProps = new HashSet<String>();
    setAllProps.add("clusterId");
    setAllProps.add("serviceId");
    setAllProps.add("componentId");
    mapProperties.put(null, setAllProps);


    TreeNode<Resource> tree = new TreeNodeImpl<Resource>(null, null, null);
    List<Resource> listResources = Collections.singletonList(componentResource);

    Map<Resource.Type, String> mapResourceIds = new HashMap<Resource.Type, String>();
    mapResourceIds.put(Resource.Type.Cluster, "clusterName");
    mapResourceIds.put(Resource.Type.Service, "serviceName");
    mapResourceIds.put(Resource.Type.Component, null);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate internalPredicate = pb.property("clusterId").equals("clusterName").and().
        property("serviceId").equals("serviceName").toPredicate();

    pb = new PredicateBuilder();
    Predicate userPredicate = pb.property("foo").equals("bar").toPredicate();
    // combine internal predicate and user predicate
    //todo: for now, need to cast to BasePredicate
    Predicate predicate = new AndPredicate((BasePredicate) internalPredicate, (BasePredicate) userPredicate);

    // expectations
    expect(componentResource.getType()).andReturn(Resource.Type.Component).anyTimes();

    expect(componentResourceInstance.getIds()).andReturn(mapResourceIds).anyTimes();
    expect(componentResourceInstance.getResourceDefinition()).andReturn(componentResourceDefinition).anyTimes();

    expect(componentResourceDefinition.getType()).andReturn(Resource.Type.Component).anyTimes();

    expect(m_controller.getSchema(Resource.Type.Component)).andReturn(componentSchema).anyTimes();
    expect(componentSchema.getCategoryProperties()).andReturn(mapProperties).anyTimes();
    expect(componentSchema.getKeyPropertyId(Resource.Type.Component)).andReturn(componentPropertyId).atLeastOnce();
    expect(componentSchema.getKeyPropertyId(Resource.Type.Cluster)).andReturn("clusterId").anyTimes();
    expect(componentSchema.getKeyPropertyId(Resource.Type.Service)).andReturn("serviceId").anyTimes();

    expect(result.getResultTree()).andReturn(tree).anyTimes();

    expect(m_controller.getResources(eq(Resource.Type.Component), eq(PropertyHelper.getReadRequest(setPropertyIds)),
        eq(predicate))).andReturn(listResources);

    replay(m_controller, result, componentResourceInstance, componentResourceDefinition, componentSchema, componentResource);

    QueryImpl query = new TestQuery(componentResourceInstance, result);
    query.setUserPredicate(userPredicate);
    query.execute();

    verify(m_controller, result, componentResourceInstance, componentResourceDefinition, componentSchema, componentResource);

    assertEquals("true", tree.getProperty("isCollection"));
    assertEquals(1, tree.getChildren().size());
    TreeNode<Resource> componentNode = tree.getChild("Component:1");
    assertSame(componentResource, componentNode.getObject());
    assertEquals(0, componentNode.getChildren().size());
  }

  @Test
  public void testAddProperty__localProperty() throws Exception {
    ResourceInstance resource = createNiceMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);
    Schema schema = createNiceMock(Schema.class);

    Map<String, Set<String>> mapSchemaProps = new HashMap<String, Set<String>>();
    mapSchemaProps.put("category", Collections.singleton("property"));
    mapSchemaProps.put(null, Collections.singleton("property2"));

    //expectations
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition).anyTimes();

    expect(resourceDefinition.getType()).andReturn(Resource.Type.Service).anyTimes();

    expect(m_controller.getSchema(Resource.Type.Service)).andReturn(schema);
    expect(schema.getCategoryProperties()).andReturn(mapSchemaProps);

    replay(m_controller, resource, resourceDefinition, schema);

    Query query = new TestQuery(resource, null);
    query.addProperty("category", "property", null);

    assertEquals(1, query.getProperties().size());
    assertEquals(Collections.singleton("property"), query.getProperties().get("category"));

    query.addProperty(null, "property2", null);

    assertEquals(2, query.getProperties().size());
    assertEquals(Collections.singleton("property2"), query.getProperties().get(null));

    verify(m_controller, resource, resourceDefinition, schema);
  }

  // this is the case where service can't differentiate category and property name
  // the category name is give as the property name
  @Test
  public void testAddProperty__localCategory_asPropertyName() throws Exception  {
    ResourceInstance resource = createNiceMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);
    Schema schema = createNiceMock(Schema.class);

    Map<String, Set<String>> mapSchemaProps = new HashMap<String, Set<String>>();
    Set<String> setProps = new HashSet<String>();
    setProps.add("property");
    setProps.add("property2");
    mapSchemaProps.put("category", setProps);
    Set<String> setInnerProps = new HashSet<String>();
    setInnerProps.add("property3");
    setInnerProps.add("property4");
    mapSchemaProps.put("category/nestedCategory", setInnerProps);
    mapSchemaProps.put(null, Collections.singleton("property5"));

    //expectations
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition).anyTimes();

    expect(resourceDefinition.getType()).andReturn(Resource.Type.Service).anyTimes();

    expect(m_controller.getSchema(Resource.Type.Service)).andReturn(schema).anyTimes();
    expect(schema.getCategoryProperties()).andReturn(mapSchemaProps).anyTimes();

    replay(m_controller, resource, resourceDefinition, schema);

    Query query = new TestQuery(resource, null);
    query.addProperty(null, "category", null);

    Map<String, Set<String>> mapProperties = query.getProperties();
    assertEquals(2, mapProperties.size());
    Set<String> setResultProps = mapProperties.get("category");
    assertEquals(2, setResultProps.size());
    assertTrue(setResultProps.contains("property"));
    assertTrue(setResultProps.contains("property2"));

    setResultProps = mapProperties.get("category/nestedCategory");
    assertEquals(2, setResultProps.size());
    assertTrue(setResultProps.contains("property3"));
    assertTrue(setResultProps.contains("property4"));

    verify(m_controller, resource, resourceDefinition, schema);
  }

  // This is the case where the service can determine that only a category was provided because it contained
  // a trailing '/'
  @Test
  public void testAddProperty__localCategory_categoryNameOnly() throws Exception {
    ResourceInstance resource = createNiceMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);
    Schema schema = createNiceMock(Schema.class);

    Map<String, Set<String>> mapSchemaProps = new HashMap<String, Set<String>>();
    Set<String> setProps = new HashSet<String>();
    setProps.add("property");
    setProps.add("property2");
    mapSchemaProps.put("category", setProps);
    Set<String> setInnerProps = new HashSet<String>();
    setInnerProps.add("property3");
    setInnerProps.add("property4");
    mapSchemaProps.put("category/nestedCategory", setInnerProps);
    mapSchemaProps.put(null, Collections.singleton("property5"));

    //expectations
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition).anyTimes();

    expect(resourceDefinition.getType()).andReturn(Resource.Type.Service).anyTimes();

    expect(m_controller.getSchema(Resource.Type.Service)).andReturn(schema).anyTimes();
    expect(schema.getCategoryProperties()).andReturn(mapSchemaProps).anyTimes();

    replay(m_controller, resource, resourceDefinition, schema);

    Query query = new TestQuery(resource, null);
    query.addProperty("category", "", null);

    Map<String, Set<String>> mapProperties = query.getProperties();
    assertEquals(2, mapProperties.size());
    Set<String> setResultProps = mapProperties.get("category");
    assertEquals(2, setResultProps.size());
    assertTrue(setResultProps.contains("property"));
    assertTrue(setResultProps.contains("property2"));

    setResultProps = mapProperties.get("category/nestedCategory");
    assertEquals(2, setResultProps.size());
    assertTrue(setResultProps.contains("property3"));
    assertTrue(setResultProps.contains("property4"));

    verify(m_controller, resource, resourceDefinition, schema);
  }

  @Test
  public void testAddProperty__localSubCategory() throws Exception {
    ResourceInstance resource = createNiceMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);
    Schema schema = createNiceMock(Schema.class);

    Map<String, Set<String>> mapSchemaProps = new HashMap<String, Set<String>>();
    Set<String> setProps = new HashSet<String>();
    setProps.add("property");
    setProps.add("property2");
    mapSchemaProps.put("category", setProps);
    Set<String> setInnerProps = new HashSet<String>();
    setInnerProps.add("property3");
    setInnerProps.add("property4");
    mapSchemaProps.put("category/nestedCategory", setInnerProps);
    mapSchemaProps.put(null, Collections.singleton("property5"));

    //expectations
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition).anyTimes();

    expect(resourceDefinition.getType()).andReturn(Resource.Type.Service).anyTimes();

    expect(m_controller.getSchema(Resource.Type.Service)).andReturn(schema).anyTimes();
    expect(schema.getCategoryProperties()).andReturn(mapSchemaProps).anyTimes();

    replay(m_controller, resource, resourceDefinition, schema);

    Query query = new TestQuery(resource, null);
    query.addProperty("category", "nestedCategory", null);

    Map<String, Set<String>> mapProperties = query.getProperties();
    assertEquals(1, mapProperties.size());
    Set<String >setResultProps = mapProperties.get("category/nestedCategory");
    assertEquals(2, setResultProps.size());
    assertTrue(setResultProps.contains("property3"));
    assertTrue(setResultProps.contains("property4"));

    verify(m_controller, resource, resourceDefinition, schema);
  }

  @Test
  public void testAddProperty__localCategorySubPropsOnly() throws Exception {
    ResourceInstance resource = createNiceMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);
    Schema schema = createNiceMock(Schema.class);

    Map<String, Set<String>> mapSchemaProps = new HashMap<String, Set<String>>();
    Set<String> setInnerProps = new HashSet<String>();
    setInnerProps.add("property3");
    setInnerProps.add("property4");
    mapSchemaProps.put("category/nestedCategory", setInnerProps);
    mapSchemaProps.put(null, Collections.singleton("property5"));

    //expectations
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition).anyTimes();

    expect(resourceDefinition.getType()).andReturn(Resource.Type.Service).anyTimes();

    expect(m_controller.getSchema(Resource.Type.Service)).andReturn(schema).anyTimes();
    expect(schema.getCategoryProperties()).andReturn(mapSchemaProps).anyTimes();

    replay(m_controller, resource, resourceDefinition, schema);

    Query query = new TestQuery(resource, null);
    query.addProperty(null, "category", null);

    Map<String, Set<String>> mapProperties = query.getProperties();
    assertEquals(2, mapProperties.size());

    assertTrue(mapProperties.get("category").isEmpty());

    Set<String> setResultProps = mapProperties.get("category/nestedCategory");
    assertEquals(2, setResultProps.size());
    assertTrue(setResultProps.contains("property3"));
    assertTrue(setResultProps.contains("property4"));

    verify(m_controller, resource, resourceDefinition, schema);
  }

  @Test
  public void testAddProperty__subProperty() throws Exception {
    ResourceInstance resource = createNiceMock(ResourceInstance.class);
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);
    ResourceInstance subResource = createNiceMock(ResourceInstance.class);
    Schema schema = createNiceMock(Schema.class);

    Map<String, Set<String>> mapSchemaProps = new HashMap<String, Set<String>>();
    mapSchemaProps.put("category", Collections.singleton("property"));
    mapSchemaProps.put(null, Collections.singleton("property2"));

    //expectations
    expect(resource.getResourceDefinition()).andReturn(resourceDefinition).anyTimes();

    expect(resourceDefinition.getType()).andReturn(Resource.Type.Service).anyTimes();

    expect(m_controller.getSchema(Resource.Type.Service)).andReturn(schema).anyTimes();
    expect(schema.getCategoryProperties()).andReturn(mapSchemaProps).anyTimes();

    expect(resource.getSubResources()).andReturn(Collections.singletonMap("components", subResource)).anyTimes();

    //todo: ensure that sub-resource was added.

    replay(m_controller, resource, resourceDefinition, subResource, schema);

    Query query = new TestQuery(resource, null);
    query.addProperty(null, "components", null);

    verify(m_controller, resource, resourceDefinition, subResource, schema);
  }

  //todo: sub-resource with property and with sub-path

//  @Test
//  public void testAddProperty__invalidProperty() {
//
//  }

  private class TestQuery extends QueryImpl {

    private Result m_result;

    public TestQuery(ResourceInstance ResourceInstance, Result result) {
      super(ResourceInstance);
      m_result = result;
    }

    @Override
    ClusterController getClusterController() {
      return m_controller;
    }

    @Override
    Result createResult() {
      return m_result;
    }
  }

  @After
  public void resetGlobalMocks() {
    reset(m_controller);
  }
}
