package org.apache.ambari.api.query;

import org.apache.ambari.api.controller.internal.PropertyIdImpl;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.api.resource.ResourceDefinition;
import org.apache.ambari.api.services.Result;
import org.apache.ambari.server.controller.spi.*;
import org.apache.ambari.api.controller.utilities.PredicateBuilder;
import org.junit.After;
import org.junit.Test;

import java.util.*;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;


/**
 * Created with IntelliJ IDEA.
 * User: john
 * Date: 9/12/12
 * Time: 12:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class QueryImplTest {

//    ClusterController m_controller = createStrictMock(ClusterController.class);
//    @Test
//    public void testExecute__Component_Instance() throws Exception {
//        ResourceDefinition componentResourceDef = createMock(ResourceDefinition.class);
//        ResourceDefinition hostComponentResourceDef = createStrictMock(ResourceDefinition.class);
//        Request request = createStrictMock(Request.class);
//        Result result = createStrictMock(Result.class);
//
//        Schema componentSchema = createMock(Schema.class);
//        Resource componentResource = createStrictMock(Resource.class);
//        Query hostComponentQuery = createStrictMock(Query.class);
//        Result hostComponentQueryResult  = createStrictMock(Result.class);
//        Resource hostComponentResource = createStrictMock(Resource.class);
//
//        List<Resource> listResources = new ArrayList<Resource>();
//        listResources.add(componentResource);
//
//        Map<Resource.Type, String> mapResourceIds = new HashMap<Resource.Type, String>();
//        mapResourceIds.put(Resource.Type.Cluster, "clusterName");
//        mapResourceIds.put(Resource.Type.Service, "serviceName");
//        mapResourceIds.put(Resource.Type.Component, "componentName");
//
//        Set<ResourceDefinition> setChildren = new HashSet<ResourceDefinition>();
//        Set<ResourceDefinition> setForeign = new HashSet<ResourceDefinition>();
//        setForeign.add(hostComponentResourceDef);
//
//        Map<String, List<Resource>> mapHostComponentResources = new HashMap<String, List<Resource>>();
//        mapHostComponentResources.put("/", Collections.singletonList(hostComponentResource));
//
//        // expectations
//        expect(componentResourceDef.getType()).andReturn(Resource.Type.Component).atLeastOnce();
//        expect(componentResourceDef.getResourceIds()).andReturn(mapResourceIds);
//        expect(m_controller.getSchema(Resource.Type.Component)).andReturn(componentSchema).atLeastOnce();
//        expect(componentSchema.getKeyPropertyId(Resource.Type.Cluster)).andReturn(new PropertyIdImpl("clusterId", "", false));
//        expect(componentSchema.getKeyPropertyId(Resource.Type.Service)).andReturn(new PropertyIdImpl("serviceId", "", false));
//        expect(componentSchema.getKeyPropertyId(Resource.Type.Component)).andReturn(new PropertyIdImpl("componentId", "", false));
//
//        expect(componentResourceDef.getId()).andReturn("componentName").atLeastOnce();
//        //expect(componentResourceDef.getChildren()).andReturn(setChildren);
//        //expect(componentResourceDef.getRelations()).andReturn(setForeign);
//        expect(hostComponentResourceDef.getQuery()).andReturn(hostComponentQuery);
//
//        PredicateBuilder pb = new PredicateBuilder();
//        Predicate predicate = pb.property("clusterId", "").equals("clusterName").and().
//                property("serviceId", "").equals("serviceName").and().
//                property("componentId", "").equals("componentName").toPredicate();
//
//        expect(m_controller.getResources(eq(Resource.Type.Component), eq(request), eq(predicate))).
//                andReturn(listResources);
//
//        result.addResources("/", listResources);
//
//        expect(hostComponentQuery.execute()).andReturn(hostComponentQueryResult);
//        expect(hostComponentQueryResult.getResources()).andReturn(mapHostComponentResources);
//        expect(hostComponentResourceDef.getId()).andReturn("hostComponentName");
//        expect(hostComponentResourceDef.getSingularName()).andReturn("host_component");
//        result.addResources("host_component", Collections.singletonList(hostComponentResource));
//
//        replay(componentResourceDef, request, result, m_controller, componentSchema, componentResource,
//                hostComponentResourceDef, hostComponentQuery, hostComponentQueryResult, hostComponentResource);
//
//        QueryImpl query = new TestQuery(componentResourceDef, result, request);
//        Result testResult = query.execute();
//        // todo: assert return value.  This is currently a mock.
//
//        verify(componentResourceDef, request, result, m_controller, componentSchema, componentResource,
//                hostComponentResourceDef, hostComponentQuery, hostComponentQueryResult, hostComponentResource);
//    }
//
//    @Test
//    public void testExecute__Component_Collection() {
//        ResourceDefinition componentResourceDef = createMock(ResourceDefinition.class);
//        Request request = createStrictMock(Request.class);
//        Result result = createStrictMock(Result.class);
//
//        Schema componentSchema = createMock(Schema.class);
//        Resource componentResource1 = createStrictMock(Resource.class);
//        Resource componentResource2 = createStrictMock(Resource.class);
//
//        List<Resource> listResources = new ArrayList<Resource>();
//        listResources.add(componentResource1);
//        listResources.add(componentResource2);
//
//        Map<Resource.Type, String> mapResourceIds = new HashMap<Resource.Type, String>();
//        mapResourceIds.put(Resource.Type.Cluster, "clusterName");
//        mapResourceIds.put(Resource.Type.Service, "serviceName");
//
//        // expectations
//        expect(componentResourceDef.getType()).andReturn(Resource.Type.Component).atLeastOnce();
//        expect(componentResourceDef.getResourceIds()).andReturn(mapResourceIds);
//        expect(m_controller.getSchema(Resource.Type.Component)).andReturn(componentSchema).atLeastOnce();
//        expect(componentSchema.getKeyPropertyId(Resource.Type.Cluster)).andReturn(new PropertyIdImpl("clusterId", "", false));
//        expect(componentSchema.getKeyPropertyId(Resource.Type.Service)).andReturn(new PropertyIdImpl("serviceId", "", false));
//
//        expect(componentResourceDef.getId()).andReturn(null).atLeastOnce();
//
//        PredicateBuilder pb = new PredicateBuilder();
//        Predicate predicate = pb.property("clusterId", "").equals("clusterName").and().
//                property("serviceId", "").equals("serviceName").toPredicate();
//
//        expect(m_controller.getResources(eq(Resource.Type.Component), eq(request), eq(predicate))).
//                andReturn(listResources);
//
//        result.addResources("/", listResources);
//
//        replay(componentResourceDef, request, result, m_controller, componentSchema, componentResource1, componentResource2);
//
//        QueryImpl query = new TestQuery(componentResourceDef, result, request);
//        Result testResult = query.execute();
//        // todo: assert return value.  This is currently a mock.
//
//        verify(componentResourceDef, request, result, m_controller, componentSchema, componentResource1, componentResource2);
//    }
//
//    @Test
//    public void testExecute__Cluster_Instance() {
//        ResourceDefinition clusterResourceDef = createMock(ResourceDefinition.class);
//        ResourceDefinition serviceResourceDef = createMock(ResourceDefinition.class);
//        ResourceDefinition hostResourceDef = createMock(ResourceDefinition.class);
//        Request request = createStrictMock(Request.class);
//        Result result = createMock(Result.class);
//
//        Schema clusterSchema = createMock(Schema.class);
//        Resource clusterResource = createStrictMock(Resource.class);
//        Query serviceQuery = createStrictMock(Query.class);
//        Result serviceQueryResult  = createStrictMock(Result.class);
//        Resource serviceResource = createStrictMock(Resource.class);
//        Resource serviceResource2 = createStrictMock(Resource.class);
//        Query hostQuery = createStrictMock(Query.class);
//        Result hostQueryResult  = createStrictMock(Result.class);
//        Resource hostResource = createStrictMock(Resource.class);
//
//        List<Resource> listResources = new ArrayList<Resource>();
//        listResources.add(clusterResource);
//
//        Map<Resource.Type, String> mapResourceIds = new HashMap<Resource.Type, String>();
//        mapResourceIds.put(Resource.Type.Cluster, "clusterName");
//
//        Set<ResourceDefinition> setChildren = new HashSet<ResourceDefinition>();
//        setChildren.add(serviceResourceDef);
//        setChildren.add(hostResourceDef);
//        Set<ResourceDefinition> setForeign = new HashSet<ResourceDefinition>();
//
//        Map<String, List<Resource>> mapServiceResources = new HashMap<String, List<Resource>>();
//        List<Resource> listServiceResources = new ArrayList<Resource>();
//        listServiceResources.add(serviceResource);
//        listServiceResources.add(serviceResource2);
//        mapServiceResources.put("/", listServiceResources);
//
//        Map<String, List<Resource>> mapHostResources = new HashMap<String, List<Resource>>();
//        mapHostResources.put("/", Collections.singletonList(hostResource));
//
//        Map<String, Set<String>> mapPropertiesAll = new HashMap<String, Set<String>>();
//        Set<String> setRootProps = new HashSet<String>();
//        Set<String> setCategoryProps = new HashSet<String>();
//        mapPropertiesAll.put(null,setRootProps);
//        mapPropertiesAll.put("category", setCategoryProps);
//
//
//        // expectations
//        expect(clusterResourceDef.getType()).andReturn(Resource.Type.Cluster).atLeastOnce();
//        expect(m_controller.getSchema(Resource.Type.Cluster)).andReturn(clusterSchema).atLeastOnce();
//        expect(clusterSchema.getCategories()).andReturn(mapPropertiesAll);
//
//        expect(clusterResourceDef.getResourceIds()).andReturn(mapResourceIds);
//        expect(clusterSchema.getKeyPropertyId(Resource.Type.Cluster)).andReturn(new PropertyIdImpl("clusterId", "", false));
//        expect(clusterResourceDef.getId()).andReturn("clusterName").atLeastOnce();
//
//
//        //expect(clusterResourceDef.getChildren()).andReturn(setChildren);
//        expect(serviceResourceDef.getQuery()).andReturn(serviceQuery);
//        expect(hostResourceDef.getQuery()).andReturn(hostQuery);
//        //expect(clusterResourceDef.getRelations()).andReturn(setForeign);
//
//        Predicate clusterEqualsPredicate = new EqualsPredicate(new PropertyIdImpl("clusterId", "", false), "clusterName");
//
//        expect(m_controller.getResources(eq(Resource.Type.Cluster), eq(request), eq(clusterEqualsPredicate))).
//                andReturn(listResources);
//
//        result.addResources("/", listResources);
//
//        expect(serviceQuery.execute()).andReturn(serviceQueryResult);
//        expect(serviceQueryResult.getResources()).andReturn(mapServiceResources);
//        expect(serviceResourceDef.getId()).andReturn(null);
//        expect(serviceResourceDef.getPluralName()).andReturn("services");
//        result.addResources("services",listServiceResources);
//
//        expect(hostQuery.execute()).andReturn(hostQueryResult);
//        expect(hostQueryResult.getResources()).andReturn(mapHostResources);
//        expect(hostResourceDef.getId()).andReturn(null);
//        expect(hostResourceDef.getPluralName()).andReturn("hosts");
//        result.addResources("hosts", Collections.singletonList(hostResource));
//
//        replay(clusterResourceDef, request, result, m_controller, clusterSchema, clusterResource,
//                serviceResourceDef, serviceQuery, serviceQueryResult, serviceResource, serviceResource2,
//                hostResourceDef, hostQuery, hostQueryResult, hostResource);
//
//        // test
//        QueryImpl query = new TestQuery(clusterResourceDef, result, request);
//        Result testResult = query.execute();
//        // todo: assert return value.  This is currently a mock.
//
//        verify(clusterResourceDef, request, result, m_controller, clusterSchema, clusterResource,
//                serviceResourceDef, serviceQuery, serviceQueryResult, serviceResource, serviceResource2,
//                hostResourceDef, hostQuery, hostQueryResult, hostResource);
//    }
//
//    private class TestQuery extends QueryImpl {
//
//        private Result m_result;
//        private Request m_request;
//
//        public TestQuery(ResourceDefinition resourceDefinition, Result result, Request request) {
//            super(resourceDefinition);
//            m_result = result;
//            m_request = request;
//        }
//
//        @Override
//        Result createResult() {
//            return m_result;
//        }
//
//        @Override
//        Request createRequest() {
//            return m_request;
//        }
//
//        @Override
//        ClusterController getClusterController() {
//            return m_controller;
//        }
//    }
//
//    @After
//    public void resetGlobalMocks() {
//        reset(m_controller);
//    }
}
