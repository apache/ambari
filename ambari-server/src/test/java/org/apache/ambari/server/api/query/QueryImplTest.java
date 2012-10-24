package org.apache.ambari.server.api.query;

import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.api.util.TreeNodeImpl;
import org.apache.ambari.server.controller.internal.PropertyIdImpl;
import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.predicate.BasePredicate;
import org.apache.ambari.server.controller.spi.*;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.api.resources.ResourceDefinition;
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


public class QueryImplTest {

  ClusterController m_controller = createMock(ClusterController.class);

  @Test
  public void testExecute__Component_instance_noSpecifiedProps() throws Exception {
    Result result = createStrictMock(Result.class);
    ResourceDefinition componentResourceDef = createMock(ResourceDefinition.class);
    ResourceDefinition hostComponentResourceDef = createStrictMock(ResourceDefinition.class);
    Schema componentSchema = createMock(Schema.class);
    Resource componentResource = createStrictMock(Resource.class);
    PropertyId componentPropertyId = PropertyHelper.getPropertyId("componentId", "");
    Query hostComponentQuery = createStrictMock(Query.class);
    Result hostComponentQueryResult = createStrictMock(Result.class);

    TreeNode<Resource> tree = new TreeNodeImpl<Resource>(null, null, null);
    TreeNode<Resource> hostComponentResultNode = new TreeNodeImpl<Resource>(null, null, null);
    List<Resource> listResources = Collections.singletonList(componentResource);

    Map<Resource.Type, String> mapResourceIds = new HashMap<Resource.Type, String>();
    mapResourceIds.put(Resource.Type.Cluster, "clusterName");
    mapResourceIds.put(Resource.Type.Service, "serviceName");
    mapResourceIds.put(Resource.Type.Component, "componentName");

    Map<String, ResourceDefinition> mapChildren = new HashMap<String, ResourceDefinition>();
    mapChildren.put("host_components", hostComponentResourceDef);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate = pb.property("clusterId", "").equals("clusterName").and().
        property("serviceId", "").equals("serviceName").and().
        property("componentId", "").equals("componentName").toPredicate();

    // expectations
    expect(componentResource.getType()).andReturn(Resource.Type.Component).anyTimes();
    expect(componentResourceDef.getId()).andReturn("componentName").atLeastOnce();
    expect(m_controller.getSchema(Resource.Type.Component)).andReturn(componentSchema).atLeastOnce();
    expect(componentSchema.getCategories()).andReturn(Collections.<String, Set<String>>emptyMap());
    expect(componentResourceDef.getSubResources()).andReturn(mapChildren);
    expect(componentResourceDef.getType()).andReturn(Resource.Type.Component).atLeastOnce();
    expect(componentResourceDef.getResourceIds()).andReturn(mapResourceIds);
    expect(componentSchema.getKeyPropertyId(Resource.Type.Cluster)).andReturn(new PropertyIdImpl("clusterId", "", false));
    expect(componentSchema.getKeyPropertyId(Resource.Type.Service)).andReturn(new PropertyIdImpl("serviceId", "", false));
    expect(componentSchema.getKeyPropertyId(Resource.Type.Component)).andReturn(componentPropertyId).atLeastOnce();

    expect(m_controller.getResources(eq(Resource.Type.Component), eq(PropertyHelper.getReadRequest(Collections.<PropertyId>emptySet())),
        eq(predicate))).andReturn(listResources);

    expect(result.getResultTree()).andReturn(tree);
    expect(componentResource.getPropertyValue(componentPropertyId)).andReturn("componentName");
    hostComponentResourceDef.setParentId(Resource.Type.Component, "componentName");
    expect(hostComponentResourceDef.getQuery()).andReturn(hostComponentQuery);
    expect(hostComponentQuery.execute()).andReturn(hostComponentQueryResult);
    expect(hostComponentQueryResult.getResultTree()).andReturn(hostComponentResultNode);


    replay(m_controller, result, componentResourceDef, hostComponentResourceDef, componentSchema, componentResource,
        hostComponentQuery, hostComponentQueryResult);

    QueryImpl query = new TestQuery(componentResourceDef, result);
    query.execute();

    verify(m_controller, result, componentResourceDef, hostComponentResourceDef, componentSchema, componentResource,
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
    Result result = createStrictMock(Result.class);
    ResourceDefinition componentResourceDef = createMock(ResourceDefinition.class);
    Schema componentSchema = createMock(Schema.class);
    Resource componentResource = createStrictMock(Resource.class);
    PropertyId componentPropertyId = PropertyHelper.getPropertyId("componentId", "");

    Map<String, Set<String>> mapProperties = new HashMap<String, Set<String>>();
    mapProperties.put("", Collections.singleton("componentId"));

    TreeNode<Resource> tree = new TreeNodeImpl<Resource>(null, null, null);
    List<Resource> listResources = Collections.singletonList(componentResource);

    Map<Resource.Type, String> mapResourceIds = new HashMap<Resource.Type, String>();
    mapResourceIds.put(Resource.Type.Cluster, "clusterName");
    mapResourceIds.put(Resource.Type.Service, "serviceName");
    mapResourceIds.put(Resource.Type.Component, "componentName");

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate = pb.property("clusterId", "").equals("clusterName").and().
        property("serviceId", "").equals("serviceName").and().
        property("componentId", "").equals("componentName").toPredicate();

    // expectations
    expect(componentResource.getType()).andReturn(Resource.Type.Component).anyTimes();
    expect(componentResourceDef.getId()).andReturn(null).atLeastOnce();
    expect(m_controller.getSchema(Resource.Type.Component)).andReturn(componentSchema).atLeastOnce();
    expect(componentSchema.getCategories()).andReturn(mapProperties);

    expect(componentSchema.getKeyPropertyId(Resource.Type.Component)).andReturn(componentPropertyId).atLeastOnce();
    expect(result.getResultTree()).andReturn(tree).atLeastOnce();

    expect(componentResourceDef.getType()).andReturn(Resource.Type.Component).atLeastOnce();
    expect(componentResourceDef.getResourceIds()).andReturn(mapResourceIds);
    expect(componentSchema.getKeyPropertyId(Resource.Type.Cluster)).andReturn(new PropertyIdImpl("clusterId", "", false));
    expect(componentSchema.getKeyPropertyId(Resource.Type.Service)).andReturn(new PropertyIdImpl("serviceId", "", false));


    expect(m_controller.getResources(eq(Resource.Type.Component), eq(PropertyHelper.getReadRequest(Collections.singleton(componentPropertyId))),
        eq(predicate))).andReturn(listResources);

    replay(m_controller, result,componentResourceDef, componentSchema, componentResource);

    QueryImpl query = new TestQuery(componentResourceDef, result);
    query.execute();

    verify(m_controller, result, componentResourceDef, componentSchema, componentResource);

    assertEquals("true", tree.getProperty("isCollection"));
    assertEquals(1, tree.getChildren().size());
    TreeNode<Resource> componentNode = tree.getChild("Component:1");
    assertSame(componentResource, componentNode.getObject());
    assertEquals(0, componentNode.getChildren().size());
  }

  @Test
  public void testExecute__collection_nullInternalPredicate_nullUserPredicate() throws Exception {
    Result result = createStrictMock(Result.class);
    ResourceDefinition clusterResourceDef = createMock(ResourceDefinition.class);
    Schema clusterSchema = createMock(Schema.class);
    Resource clusterResource = createStrictMock(Resource.class);
    PropertyId clusterPropertyId = PropertyHelper.getPropertyId("clusterId", "");

    Map<String, Set<String>> mapProperties = new HashMap<String, Set<String>>();
    mapProperties.put("", Collections.singleton("clusterId"));

    TreeNode<Resource> tree = new TreeNodeImpl<Resource>(null, null, null);
    List<Resource> listResources = Collections.singletonList(clusterResource);

    Map<Resource.Type, String> mapResourceIds = new HashMap<Resource.Type, String>();

    // expectations
    expect(clusterResource.getType()).andReturn(Resource.Type.Cluster).anyTimes();
    expect(clusterResourceDef.getId()).andReturn(null).atLeastOnce();
    expect(m_controller.getSchema(Resource.Type.Component)).andReturn(clusterSchema).atLeastOnce();
    expect(clusterSchema.getCategories()).andReturn(mapProperties);

    expect(clusterSchema.getKeyPropertyId(Resource.Type.Component)).andReturn(clusterPropertyId).atLeastOnce();
    expect(result.getResultTree()).andReturn(tree).atLeastOnce();

    expect(clusterResourceDef.getType()).andReturn(Resource.Type.Component).atLeastOnce();
    expect(clusterResourceDef.getResourceIds()).andReturn(mapResourceIds);

    expect(m_controller.getResources(eq(Resource.Type.Component), eq(PropertyHelper.getReadRequest(Collections.singleton(clusterPropertyId))),
        (Predicate) isNull())).andReturn(listResources);


    replay(m_controller, result, clusterResourceDef, clusterSchema, clusterResource);

    QueryImpl query = new TestQuery(clusterResourceDef, result);
    query.execute();

    verify(m_controller, result, clusterResourceDef, clusterSchema, clusterResource);

    assertEquals("true", tree.getProperty("isCollection"));
    assertEquals(1, tree.getChildren().size());
    TreeNode<Resource> clusterNode = tree.getChild("Cluster:1");
    assertSame(clusterResource, clusterNode.getObject());
    assertEquals(0, clusterNode.getChildren().size());

  }

  @Test
  public void testExecute__collection_nullInternalPredicate_nonNullUserPredicate() throws Exception {
    Result result = createStrictMock(Result.class);
    ResourceDefinition clusterResourceDef = createMock(ResourceDefinition.class);
    Schema clusterSchema = createMock(Schema.class);
    Resource clusterResource = createStrictMock(Resource.class);
    PropertyId clusterPropertyId = PropertyHelper.getPropertyId("clusterId", "");
    Predicate userPredicate = createMock(Predicate.class);

    Map<String, Set<String>> mapProperties = new HashMap<String, Set<String>>();
    mapProperties.put("", Collections.singleton("clusterId"));

    TreeNode<Resource> tree = new TreeNodeImpl<Resource>(null, null, null);
    List<Resource> listResources = Collections.singletonList(clusterResource);

    Map<Resource.Type, String> mapResourceIds = new HashMap<Resource.Type, String>();

    // expectations
    expect(clusterResource.getType()).andReturn(Resource.Type.Cluster).anyTimes();
    expect(clusterResourceDef.getId()).andReturn(null).atLeastOnce();
    expect(m_controller.getSchema(Resource.Type.Component)).andReturn(clusterSchema).atLeastOnce();
    expect(clusterSchema.getCategories()).andReturn(mapProperties);

    expect(clusterSchema.getKeyPropertyId(Resource.Type.Component)).andReturn(clusterPropertyId).atLeastOnce();
    expect(result.getResultTree()).andReturn(tree).atLeastOnce();

    expect(clusterResourceDef.getType()).andReturn(Resource.Type.Component).atLeastOnce();
    expect(clusterResourceDef.getResourceIds()).andReturn(mapResourceIds);

    expect(m_controller.getResources(eq(Resource.Type.Component), eq(PropertyHelper.getReadRequest(Collections.singleton(clusterPropertyId))),
        eq(userPredicate))).andReturn(listResources);

    replay(m_controller, result,clusterResourceDef, clusterSchema, clusterResource, userPredicate);

    QueryImpl query = new TestQuery(clusterResourceDef, result);
    query.setUserPredicate(userPredicate);
    query.execute();

    verify(m_controller, result, clusterResourceDef, clusterSchema, clusterResource, userPredicate);

    assertEquals("true", tree.getProperty("isCollection"));
    assertEquals(1, tree.getChildren().size());
    TreeNode<Resource> clusterNode = tree.getChild("Cluster:1");
    assertSame(clusterResource, clusterNode.getObject());
    assertEquals(0, clusterNode.getChildren().size());
  }

  @Test
  public void testExecute__collection_nonNullInternalPredicate_nonNullUserPredicate() throws Exception {
    Result result = createStrictMock(Result.class);
    ResourceDefinition componentResourceDef = createMock(ResourceDefinition.class);
    Schema componentSchema = createMock(Schema.class);
    Resource componentResource = createStrictMock(Resource.class);
    PropertyId componentPropertyId = PropertyHelper.getPropertyId("componentId", "");

    Map<String, Set<String>> mapProperties = new HashMap<String, Set<String>>();
    mapProperties.put("", Collections.singleton("componentId"));

    TreeNode<Resource> tree = new TreeNodeImpl<Resource>(null, null, null);
    List<Resource> listResources = Collections.singletonList(componentResource);

    Map<Resource.Type, String> mapResourceIds = new HashMap<Resource.Type, String>();
    mapResourceIds.put(Resource.Type.Cluster, "clusterName");
    mapResourceIds.put(Resource.Type.Service, "serviceName");
    mapResourceIds.put(Resource.Type.Component, "componentName");

    PredicateBuilder pb = new PredicateBuilder();
    Predicate internalPredicate = pb.property("clusterId", "").equals("clusterName").and().
        property("serviceId", "").equals("serviceName").and().
        property("componentId", "").equals("componentName").toPredicate();

    pb = new PredicateBuilder();
    Predicate userPredicate = pb.property("foo", "").equals("bar").toPredicate();
    // combine internal predicate and user predicate
    //todo: for now, need to cast to BasePredicate
    Predicate predicate = new AndPredicate((BasePredicate) internalPredicate, (BasePredicate) userPredicate);

    // expectations
    expect(componentResource.getType()).andReturn(Resource.Type.Component).anyTimes();
    expect(componentResourceDef.getId()).andReturn(null).atLeastOnce();
    expect(m_controller.getSchema(Resource.Type.Component)).andReturn(componentSchema).atLeastOnce();
    expect(componentSchema.getCategories()).andReturn(mapProperties);

    expect(componentSchema.getKeyPropertyId(Resource.Type.Component)).andReturn(componentPropertyId).atLeastOnce();
    expect(result.getResultTree()).andReturn(tree).atLeastOnce();

    expect(componentResourceDef.getType()).andReturn(Resource.Type.Component).atLeastOnce();
    expect(componentResourceDef.getResourceIds()).andReturn(mapResourceIds);
    expect(componentSchema.getKeyPropertyId(Resource.Type.Cluster)).andReturn(new PropertyIdImpl("clusterId", "", false));
    expect(componentSchema.getKeyPropertyId(Resource.Type.Service)).andReturn(new PropertyIdImpl("serviceId", "", false));


    expect(m_controller.getResources(eq(Resource.Type.Component), eq(PropertyHelper.getReadRequest(Collections.singleton(componentPropertyId))),
        eq(predicate))).andReturn(listResources);

    replay(m_controller, result, componentResourceDef, componentSchema, componentResource);

    QueryImpl query = new TestQuery(componentResourceDef, result);
    query.setUserPredicate(userPredicate);
    query.execute();

    verify(m_controller, result, componentResourceDef, componentSchema, componentResource);

    assertEquals("true", tree.getProperty("isCollection"));
    assertEquals(1, tree.getChildren().size());
    TreeNode<Resource> componentNode = tree.getChild("Component:1");
    assertSame(componentResource, componentNode.getObject());
    assertEquals(0, componentNode.getChildren().size());
  }

  @Test
  public void testAddProperty__localProperty() {
    ResourceDefinition resource = createMock(ResourceDefinition.class);
    Schema schema = createMock(Schema.class);

    Map<String, Set<String>> mapSchemaProps = new HashMap<String, Set<String>>();
    mapSchemaProps.put("category", Collections.singleton("property"));
    mapSchemaProps.put(null, Collections.singleton("property2"));

    //expectations
    expect(resource.getType()).andReturn(Resource.Type.Service).atLeastOnce();
    expect(m_controller.getSchema(Resource.Type.Service)).andReturn(schema);
    expect(schema.getCategories()).andReturn(mapSchemaProps);

    replay(m_controller, resource, schema);

    Query query = new TestQuery(resource, null);
    query.addProperty("category", "property", null);

    assertEquals(1, query.getProperties().size());
    assertEquals(Collections.singleton("property"), query.getProperties().get("category"));

    query.addProperty(null, "property2", null);

    assertEquals(2, query.getProperties().size());
    assertEquals(Collections.singleton("property2"), query.getProperties().get(null));

    verify(m_controller, resource, schema);
  }

  @Test
  public void testAddProperty__localCategory() {
    ResourceDefinition resource = createMock(ResourceDefinition.class);
    Schema schema = createMock(Schema.class);

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
    expect(resource.getType()).andReturn(Resource.Type.Service).atLeastOnce();
    expect(m_controller.getSchema(Resource.Type.Service)).andReturn(schema);
    expect(schema.getCategories()).andReturn(mapSchemaProps);

    replay(m_controller, resource, schema);

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

    verify(m_controller, resource, schema);
  }

  @Test
  public void testAddProperty__localSubCategory() {
    ResourceDefinition resource = createMock(ResourceDefinition.class);
    Schema schema = createMock(Schema.class);

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
    expect(resource.getType()).andReturn(Resource.Type.Service).atLeastOnce();
    expect(m_controller.getSchema(Resource.Type.Service)).andReturn(schema);
    expect(schema.getCategories()).andReturn(mapSchemaProps);

    replay(m_controller, resource, schema);

    Query query = new TestQuery(resource, null);
    query.addProperty("category", "nestedCategory", null);

    Map<String, Set<String>> mapProperties = query.getProperties();
    assertEquals(1, mapProperties.size());
    Set<String >setResultProps = mapProperties.get("category/nestedCategory");
    assertEquals(2, setResultProps.size());
    assertTrue(setResultProps.contains("property3"));
    assertTrue(setResultProps.contains("property4"));

    verify(m_controller, resource, schema);
  }

  @Test
  public void testAddProperty__localCategorySubPropsOnly() {
    ResourceDefinition resource = createMock(ResourceDefinition.class);
    Schema schema = createMock(Schema.class);

    Map<String, Set<String>> mapSchemaProps = new HashMap<String, Set<String>>();
    Set<String> setInnerProps = new HashSet<String>();
    setInnerProps.add("property3");
    setInnerProps.add("property4");
    mapSchemaProps.put("category/nestedCategory", setInnerProps);
    mapSchemaProps.put(null, Collections.singleton("property5"));

    //expectations
    expect(resource.getType()).andReturn(Resource.Type.Service).atLeastOnce();
    expect(m_controller.getSchema(Resource.Type.Service)).andReturn(schema);
    expect(schema.getCategories()).andReturn(mapSchemaProps);

    replay(m_controller, resource, schema);

    Query query = new TestQuery(resource, null);
    query.addProperty(null, "category", null);

    Map<String, Set<String>> mapProperties = query.getProperties();
    assertEquals(2, mapProperties.size());

    assertTrue(mapProperties.get("category").isEmpty());

    Set<String> setResultProps = mapProperties.get("category/nestedCategory");
    assertEquals(2, setResultProps.size());
    assertTrue(setResultProps.contains("property3"));
    assertTrue(setResultProps.contains("property4"));

    verify(m_controller, resource, schema);
  }

  @Test
  public void testAddProperty__subProperty() {
    ResourceDefinition resource = createMock(ResourceDefinition.class);
    ResourceDefinition subResource = createMock(ResourceDefinition.class);
    Schema schema = createMock(Schema.class);

    Map<String, Set<String>> mapSchemaProps = new HashMap<String, Set<String>>();
    mapSchemaProps.put("category", Collections.singleton("property"));
    mapSchemaProps.put(null, Collections.singleton("property2"));

    //expectations
    expect(resource.getType()).andReturn(Resource.Type.Service).atLeastOnce();
    expect(m_controller.getSchema(Resource.Type.Service)).andReturn(schema);
    expect(schema.getCategories()).andReturn(mapSchemaProps);

    expect(resource.getSubResources()).andReturn(Collections.singletonMap("components", subResource));

    //todo: ensure that sub-resource was added.

    replay(m_controller, resource, subResource, schema);

    Query query = new TestQuery(resource, null);
    query.addProperty(null, "components", null);

    verify(m_controller, resource, subResource, schema);
  }

  //todo: sub-resource with property and with sub-path

//  @Test
//  public void testAddProperty__invalidProperty() {
//
//  }

  private class TestQuery extends QueryImpl {

    private Result m_result;

    public TestQuery(ResourceDefinition resourceDefinition, Result result) {
      super(resourceDefinition);
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
