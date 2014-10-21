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

package org.apache.ambari.server.api.query.render;

import org.apache.ambari.server.api.query.QueryInfo;
import org.apache.ambari.server.api.resources.ClusterResourceDefinition;
import org.apache.ambari.server.api.resources.HostComponentResourceDefinition;
import org.apache.ambari.server.api.resources.HostResourceDefinition;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.api.util.TreeNodeImpl;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.state.PropertyInfo;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * ClusterBlueprintRenderer unit tests.
 */
public class ClusterBlueprintRendererTest {
  @Test
  public void testFinalizeProperties__instance() {
    QueryInfo rootQuery = new QueryInfo(new ClusterResourceDefinition(), new HashSet<String>());
    TreeNode<QueryInfo> queryTree = new TreeNodeImpl<QueryInfo>(null, rootQuery, "Cluster");
    rootQuery.getProperties().add("foo/bar");
    rootQuery.getProperties().add("prop1");

    QueryInfo hostInfo = new QueryInfo(new HostResourceDefinition(), new HashSet<String>());
    queryTree.addChild(hostInfo, "Host");

    QueryInfo hostComponentInfo = new QueryInfo(new HostComponentResourceDefinition(), new HashSet<String>());
    queryTree.getChild("Host").addChild(hostComponentInfo, "HostComponent");

    ClusterBlueprintRenderer renderer = new ClusterBlueprintRenderer();
    TreeNode<Set<String>> propertyTree = renderer.finalizeProperties(queryTree, false);

    Set<String> rootProperties = propertyTree.getObject();
    assertEquals(2, rootProperties.size());
    assertNotNull(propertyTree.getChild("Host"));
    assertTrue(propertyTree.getChild("Host").getObject().isEmpty());
    assertNotNull(propertyTree.getChild("Host/HostComponent"));
    assertEquals(1, propertyTree.getChild("Host/HostComponent").getObject().size());
    assertTrue(propertyTree.getChild("Host/HostComponent").getObject().contains("HostRoles/component_name"));
  }

  @Test
  public void testFinalizeProperties__instance_noComponentNode() {
    QueryInfo rootQuery = new QueryInfo(new ClusterResourceDefinition(), new HashSet<String>());
    TreeNode<QueryInfo> queryTree = new TreeNodeImpl<QueryInfo>(null, rootQuery, "Cluster");
    rootQuery.getProperties().add("foo/bar");
    rootQuery.getProperties().add("prop1");

    ClusterBlueprintRenderer renderer = new ClusterBlueprintRenderer();
    TreeNode<Set<String>> propertyTree = renderer.finalizeProperties(queryTree, false);

    Set<String> rootProperties = propertyTree.getObject();
    assertEquals(2, rootProperties.size());
    assertNotNull(propertyTree.getChild("Host"));
    assertTrue(propertyTree.getChild("Host").getObject().isEmpty());
    assertNotNull(propertyTree.getChild("Host/HostComponent"));
    assertEquals(1, propertyTree.getChild("Host/HostComponent").getObject().size());
    assertTrue(propertyTree.getChild("Host/HostComponent").getObject().contains("HostRoles/component_name"));
  }

  @Test
  public void testFinalizeResult() throws Exception{

    AmbariManagementController controller = createMock(AmbariManagementController.class);
    AmbariMetaInfo stackInfo = createNiceMock(AmbariMetaInfo.class);

    expect(stackInfo.getRequiredProperties("HDP", "1.3.3", "HDFS")).andReturn(Collections.<String, PropertyInfo>emptyMap());
    expect(stackInfo.getRequiredProperties("HDP", "1.3.3", "MAPREDUCE")).andReturn(Collections.<String, PropertyInfo>emptyMap());

    Result result = new ResultImpl(true);
    createClusterResultTree(result.getResultTree());

    ClusterBlueprintRenderer renderer = new TestBlueprintRenderer(controller);
    Result blueprintResult = renderer.finalizeResult(result);

    TreeNode<Resource> blueprintTree = blueprintResult.getResultTree();
    assertNull(blueprintTree.getProperty("isCollection"));
    assertEquals(1, blueprintTree.getChildren().size());

    TreeNode<Resource> blueprintNode = blueprintTree.getChildren().iterator().next();
    assertEquals(0, blueprintNode.getChildren().size());
    Resource blueprintResource = blueprintNode.getObject();
    Map<String, Map<String, Object>> properties = blueprintResource.getPropertiesMap();

    assertEquals("HDP", properties.get("Blueprints").get("stack_name"));
    assertEquals("1.3.3", properties.get("Blueprints").get("stack_version"));

    Collection<Map<String, Object>> host_groups = (Collection<Map<String, Object>>) properties.get("").get("host_groups");
    assertEquals(2, host_groups.size());

    for (Map<String, Object> hostGroupProperties : host_groups) {
      String host_group_name = (String) hostGroupProperties.get("name");
      if (host_group_name.equals("host_group_1")) {
        assertEquals("1", hostGroupProperties.get("cardinality"));

        Collection<Map<String, String>> components = (Collection<Map<String, String>>) hostGroupProperties.get("components");
        // 4 specified components and ambari server
        assertEquals(5, components.size());

        Set<String> expectedValues = new HashSet<String>(
            Arrays.asList("JOBTRACKER", "TASKTRACKER", "NAMENODE", "DATANODE", "AMBARI_SERVER"));

        Set<String> actualValues = new HashSet<String>();


        for (Map<String, String> componentProperties : components) {
          assertEquals(1, componentProperties.size());
          actualValues.add(componentProperties.get("name"));
        }
        assertEquals(expectedValues, actualValues);
      } else if (host_group_name.equals("host_group_2")) {
        // cardinality is 2 because 2 hosts share same topology
        assertEquals("2", hostGroupProperties.get("cardinality"));

        Collection<Map<String, String>> components = (Collection<Map<String, String>>) hostGroupProperties.get("components");
        assertEquals(2, components.size());

        Set<String> expectedValues = new HashSet<String>(
            Arrays.asList("TASKTRACKER", "DATANODE"));

        Set<String> actualValues = new HashSet<String>();


        for (Map<String, String> componentProperties : components) {
          assertEquals(1, componentProperties.size());
          actualValues.add(componentProperties.get("name"));
        }
        assertEquals(expectedValues, actualValues);
      }
    }
  }

  //todo: collection resource

  private void createClusterResultTree(TreeNode<Resource> resultTree) throws Exception{
    Resource clusterResource = new ResourceImpl(Resource.Type.Cluster) {
      @Override
      public Map<String, Map<String, Object>> getPropertiesMap() {
        Map<String, Map<String, Object>> originalMap =
          super.getPropertiesMap();

        // override the properties map for simpler testing
        originalMap.put("Clusters/desired_configs", Collections.<String, Object>emptyMap());

        return originalMap;
      };

    };

    clusterResource.setProperty("Clusters/cluster_name", "testCluster");
    clusterResource.setProperty("Clusters/version", "HDP-1.3.3");

    TreeNode<Resource> clusterTree = resultTree.addChild(clusterResource, "Cluster:1");

    // add empty services resource for basic unit testing
    Resource servicesResource = new ResourceImpl(Resource.Type.Service);
    clusterTree.addChild(servicesResource, "services");


    Resource configurationsResource = new ResourceImpl(Resource.Type.Configuration);
    TreeNode<Resource> configurations = clusterTree.addChild(configurationsResource, "configurations");
    Resource resourceOne = new ResourceImpl(Resource.Type.Configuration) {
      @Override
      public Map<String, Map<String, Object>> getPropertiesMap() {
        Map<String, Map<String, Object>> originalMap =
        super.getPropertiesMap();

        // return null for properties, to simulate upgrade case
        originalMap.put("properties", null);

        return originalMap;
      }
    };

    resourceOne.setProperty("type", "mapreduce-log4j");

    configurations.addChild(resourceOne, "resourceOne");


    Resource resourceTwo = new ResourceImpl(Resource.Type.Configuration) {
      @Override
      public Map<String, Map<String, Object>> getPropertiesMap() {
        Map<String, Map<String, Object>> originalMap =
          super.getPropertiesMap();

        // return test properties, to simluate valid configuration entry
        originalMap.put("properties", Collections.<String, Object>singletonMap("propertyOne", "valueOne"));

        return originalMap;
      }
    };

    resourceTwo.setProperty("type", "test-type-one");

    configurations.addChild(resourceTwo, "resourceTwo");

    Resource blueprintOne = new ResourceImpl(Resource.Type.Blueprint);
    blueprintOne.setProperty("Blueprints/blueprint_name", "blueprint-testCluster");
    clusterTree.addChild(blueprintOne, "Blueprints");

    TreeNode<Resource> hostsTree = clusterTree.addChild(null, "hosts");
    hostsTree.setProperty("isCollection", "true");

    // host 1 : ambari host
    Resource hostResource = new ResourceImpl(Resource.Type.Host);
    hostResource.setProperty("Hosts/host_name", getLocalHostName());
    TreeNode<Resource> hostTree = hostsTree.addChild(hostResource, "Host:1");

    TreeNode<Resource> hostComponentsTree = hostTree.addChild(null, "host_components");
    hostComponentsTree.setProperty("isCollection", "true");

    // host 1 components
    Resource nnComponentResource = new ResourceImpl(Resource.Type.HostComponent);
    nnComponentResource.setProperty("HostRoles/component_name", "NAMENODE");

    Resource dnComponentResource = new ResourceImpl(Resource.Type.HostComponent);
    dnComponentResource.setProperty("HostRoles/component_name", "DATANODE");

    Resource jtComponentResource = new ResourceImpl(Resource.Type.HostComponent);
    jtComponentResource.setProperty("HostRoles/component_name", "JOBTRACKER");

    Resource ttComponentResource = new ResourceImpl(Resource.Type.HostComponent);
    ttComponentResource.setProperty("HostRoles/component_name", "TASKTRACKER");

    hostComponentsTree.addChild(nnComponentResource, "HostComponent:1");
    hostComponentsTree.addChild(dnComponentResource, "HostComponent:2");
    hostComponentsTree.addChild(jtComponentResource, "HostComponent:3");
    hostComponentsTree.addChild(ttComponentResource, "HostComponent:4");

    // host 2
    Resource host2Resource = new ResourceImpl(Resource.Type.Host);
    host2Resource.setProperty("Hosts/host_name", "testHost2");
    TreeNode<Resource> host2Tree = hostsTree.addChild(host2Resource, "Host:2");

    TreeNode<Resource> host2ComponentsTree = host2Tree.addChild(null, "host_components");
    host2ComponentsTree.setProperty("isCollection", "true");

    // host 2 components
    host2ComponentsTree.addChild(dnComponentResource, "HostComponent:1");
    host2ComponentsTree.addChild(ttComponentResource, "HostComponent:2");

    // host 3 : same topology as host 2
    Resource host3Resource = new ResourceImpl(Resource.Type.Host);
    host3Resource.setProperty("Hosts/host_name", "testHost3");
    TreeNode<Resource> host3Tree = hostsTree.addChild(host3Resource, "Host:3");

    TreeNode<Resource> host3ComponentsTree = host3Tree.addChild(null, "host_components");
    host3ComponentsTree.setProperty("isCollection", "true");

    // host 3 components
    host3ComponentsTree.addChild(dnComponentResource, "HostComponent:1");
    host3ComponentsTree.addChild(ttComponentResource, "HostComponent:2");
  }

  private String getLocalHostName() throws UnknownHostException {
    return InetAddress.getLocalHost().getHostName();
  }

  private static class TestBlueprintRenderer extends ClusterBlueprintRenderer {

    private AmbariManagementController testController;

    private TestBlueprintRenderer(AmbariManagementController controller) {
      testController = controller;
    }

    @Override
    protected AmbariManagementController getController() {
      return testController;
    }
  }
}
