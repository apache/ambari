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


package org.apache.ambari.server.api.resources;


import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.internal.ClusterControllerImpl;
import org.apache.ambari.server.controller.internal.ClusterControllerImplTest;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * ResourceInstanceImpl unit tests.
 */
public class ResourceInstanceImplTest {

  @Test
  public void testIsCollection__True() {
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);

    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.Cluster, "cluster");
    mapIds.put(Resource.Type.Service, null);

    // expectations
    expect(resourceDefinition.getType()).andReturn(Resource.Type.Service).anyTimes();
    expect(resourceDefinition.getSubResourceDefinitions()).andReturn(Collections.<SubResourceDefinition>emptySet()).anyTimes();

    replay(resourceDefinition);

    //test
    ResourceInstance instance = new TestQuery(mapIds, resourceDefinition);
    assertTrue(instance.isCollectionResource());

    verify(resourceDefinition);
  }

  @Test
  public void testIsCollection__False() {
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);

    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.Cluster, "cluster");
    mapIds.put(Resource.Type.Service, "service");

    // expectations
    expect(resourceDefinition.getType()).andReturn(Resource.Type.Service).anyTimes();
    expect(resourceDefinition.getSubResourceDefinitions()).andReturn(Collections.<SubResourceDefinition>emptySet()).anyTimes();

    replay(resourceDefinition);

    //test
    ResourceInstance instance = new TestQuery(mapIds, resourceDefinition);
    assertFalse(instance.isCollectionResource());

    verify(resourceDefinition);
  }

  @Test
  public void testExecute__Cluster_instance_noSpecifiedProps() throws Exception {
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);

    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.Cluster, "cluster");

    // expectations
    expect(resourceDefinition.getType()).andReturn(Resource.Type.Cluster).anyTimes();
    Set<SubResourceDefinition> setChildren = new HashSet<SubResourceDefinition>();
    setChildren.add(new SubResourceDefinition(Resource.Type.Host));

    expect(resourceDefinition.getSubResourceDefinitions()).andReturn(setChildren).anyTimes();

    replay(resourceDefinition);

    //test
    ResourceInstanceImpl instance = new TestQuery(mapIds, resourceDefinition);

    Result result = instance.execute();

    verify(resourceDefinition);

    TreeNode<Resource> tree = result.getResultTree();

    Assert.assertEquals(1, tree.getChildren().size());
    TreeNode<Resource> clusterNode = tree.getChild("Cluster:1");
    Assert.assertEquals("Cluster:1", clusterNode.getName());
    Assert.assertEquals(Resource.Type.Cluster, clusterNode.getObject().getType());
    Assert.assertEquals(1, clusterNode.getChildren().size());
    TreeNode<Resource> hostNode = clusterNode.getChild("hosts");
    Assert.assertEquals(4, hostNode.getChildren().size());
  }

  @Test
 public void testExecute__Host_collection_noSpecifiedProps() throws Exception {
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);

    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();

    // expectations
    expect(resourceDefinition.getType()).andReturn(Resource.Type.Host).anyTimes();
    Set<SubResourceDefinition> setChildren = new HashSet<SubResourceDefinition>();

    expect(resourceDefinition.getSubResourceDefinitions()).andReturn(setChildren).anyTimes();

    replay(resourceDefinition);

    //test
    ResourceInstanceImpl instance = new TestQuery(mapIds, resourceDefinition);

    Result result = instance.execute();

    verify(resourceDefinition);

    TreeNode<Resource> tree = result.getResultTree();

    Assert.assertEquals(4, tree.getChildren().size());
    TreeNode<Resource> hostNode = tree.getChild("Host:1");
    Assert.assertEquals("Host:1", hostNode.getName());
    Assert.assertEquals(Resource.Type.Host, hostNode.getObject().getType());

    hostNode = tree.getChild("Host:2");
    Assert.assertEquals("Host:2", hostNode.getName());
    Assert.assertEquals(Resource.Type.Host, hostNode.getObject().getType());

    hostNode = tree.getChild("Host:3");
    Assert.assertEquals("Host:3", hostNode.getName());
    Assert.assertEquals(Resource.Type.Host, hostNode.getObject().getType());

    hostNode = tree.getChild("Host:4");
    Assert.assertEquals("Host:4", hostNode.getName());
    Assert.assertEquals(Resource.Type.Host, hostNode.getObject().getType());
  }

  @Test
  public void testExecute__collection_nullInternalPredicate_nullUserPredicate() throws Exception {
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);

    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();

    // expectations
    expect(resourceDefinition.getType()).andReturn(Resource.Type.Cluster).anyTimes();
    Set<SubResourceDefinition> setChildren = new HashSet<SubResourceDefinition>();
    setChildren.add(new SubResourceDefinition(Resource.Type.Host));

    expect(resourceDefinition.getSubResourceDefinitions()).andReturn(setChildren).anyTimes();

    replay(resourceDefinition);

    //test
    ResourceInstanceImpl instance = new TestQuery(mapIds, resourceDefinition);

    Result result = instance.execute();

    verify(resourceDefinition);

    TreeNode<Resource> tree = result.getResultTree();

    Assert.assertEquals(1, tree.getChildren().size());
    TreeNode<Resource> clusterNode = tree.getChild("Cluster:1");
    Assert.assertEquals("Cluster:1", clusterNode.getName());
    Assert.assertEquals(Resource.Type.Cluster, clusterNode.getObject().getType());
    Assert.assertEquals(0, clusterNode.getChildren().size());
  }

  @Test
  public void testExecute__collection_nullInternalPredicate_nonNullUserPredicate() throws Exception {
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);

    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.Cluster, "cluster");

    // expectations
    expect(resourceDefinition.getType()).andReturn(Resource.Type.Host).anyTimes();
    Set<SubResourceDefinition> setChildren = new HashSet<SubResourceDefinition>();

    expect(resourceDefinition.getSubResourceDefinitions()).andReturn(setChildren).anyTimes();

    replay(resourceDefinition);

    //test
    ResourceInstanceImpl instance = new TestQuery(mapIds, resourceDefinition);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate = pb.property("Hosts/host_name").equals("host:2").toPredicate();

    instance.setUserPredicate(predicate);

    Result result = instance.execute();

    verify(resourceDefinition);

    TreeNode<Resource> tree = result.getResultTree();

    Assert.assertEquals(1, tree.getChildren().size());
    TreeNode<Resource> hostNode = tree.getChild("Host:1");
    Assert.assertEquals("Host:1", hostNode.getName());
    Assert.assertEquals(Resource.Type.Host, hostNode.getObject().getType());
    Assert.assertEquals("host:2", hostNode.getObject().getPropertyValue("Hosts/host_name"));
  }

  @Test
  public void testExecute__collection_nonNullInternalPredicate_nonNullUserPredicate() throws Exception {
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);

    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();

    // expectations
    expect(resourceDefinition.getType()).andReturn(Resource.Type.Host).anyTimes();
    Set<SubResourceDefinition> setChildren = new HashSet<SubResourceDefinition>();

    expect(resourceDefinition.getSubResourceDefinitions()).andReturn(setChildren).anyTimes();

    replay(resourceDefinition);

    //test
    ResourceInstanceImpl instance = new TestQuery(mapIds, resourceDefinition);

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate = pb.property("Hosts/host_name").equals("host:2").toPredicate();

    instance.setUserPredicate(predicate);

    Result result = instance.execute();

    verify(resourceDefinition);

    TreeNode<Resource> tree = result.getResultTree();

    Assert.assertEquals(1, tree.getChildren().size());
    TreeNode<Resource> hostNode = tree.getChild("Host:1");
    Assert.assertEquals("Host:1", hostNode.getName());
    Assert.assertEquals(Resource.Type.Host, hostNode.getObject().getType());
    Assert.assertEquals("host:2", hostNode.getObject().getPropertyValue("Hosts/host_name"));  }

  @Test
  public void testAddProperty__localProperty() throws Exception {
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);

    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();

    // expectations
    expect(resourceDefinition.getType()).andReturn(Resource.Type.Host).anyTimes();
    Set<SubResourceDefinition> setChildren = new HashSet<SubResourceDefinition>();

    expect(resourceDefinition.getSubResourceDefinitions()).andReturn(setChildren).anyTimes();

    replay(resourceDefinition);

    //test
    ResourceInstanceImpl instance = new TestQuery(mapIds, resourceDefinition);

    instance.addLocalProperty("c1/p1");

    Result result = instance.execute();

    verify(resourceDefinition);

    TreeNode<Resource> tree = result.getResultTree();

    Assert.assertEquals(4, tree.getChildren().size());
    TreeNode<Resource> hostNode = tree.getChild("Host:1");
    Assert.assertEquals("Host:1", hostNode.getName());
    Assert.assertEquals(Resource.Type.Host, hostNode.getObject().getType());
    Assert.assertNotNull(hostNode.getObject().getPropertyValue("c1/p1"));

    hostNode = tree.getChild("Host:2");
    Assert.assertEquals("Host:2", hostNode.getName());
    Assert.assertEquals(Resource.Type.Host, hostNode.getObject().getType());
    Assert.assertNotNull(hostNode.getObject().getPropertyValue("c1/p1"));

    hostNode = tree.getChild("Host:3");
    Assert.assertEquals("Host:3", hostNode.getName());
    Assert.assertEquals(Resource.Type.Host, hostNode.getObject().getType());
    Assert.assertNotNull(hostNode.getObject().getPropertyValue("c1/p1"));

    hostNode = tree.getChild("Host:4");
    Assert.assertEquals("Host:4", hostNode.getName());
    Assert.assertEquals(Resource.Type.Host, hostNode.getObject().getType());
    Assert.assertNotNull(hostNode.getObject().getPropertyValue("c1/p1"));
  }

  @Test
  public void testAddProperty__allCategoryProperties() throws Exception {
    ResourceDefinition resourceDefinition = createNiceMock(ResourceDefinition.class);

    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();

    // expectations
    expect(resourceDefinition.getType()).andReturn(Resource.Type.Host).anyTimes();
    Set<SubResourceDefinition> setChildren = new HashSet<SubResourceDefinition>();

    expect(resourceDefinition.getSubResourceDefinitions()).andReturn(setChildren).anyTimes();

    replay(resourceDefinition);

    //test
    ResourceInstanceImpl instance = new TestQuery(mapIds, resourceDefinition);

    instance.addLocalProperty("c1");

    Result result = instance.execute();

    verify(resourceDefinition);

    TreeNode<Resource> tree = result.getResultTree();

    Assert.assertEquals(4, tree.getChildren().size());
    TreeNode<Resource> hostNode = tree.getChild("Host:1");
    Assert.assertEquals("Host:1", hostNode.getName());
    Assert.assertEquals(Resource.Type.Host, hostNode.getObject().getType());
    Assert.assertNotNull(hostNode.getObject().getPropertyValue("c1/p1"));
    Assert.assertNotNull(hostNode.getObject().getPropertyValue("c1/p2"));
    Assert.assertNotNull(hostNode.getObject().getPropertyValue("c1/p3"));

    hostNode = tree.getChild("Host:2");
    Assert.assertEquals("Host:2", hostNode.getName());
    Assert.assertEquals(Resource.Type.Host, hostNode.getObject().getType());
    Assert.assertNotNull(hostNode.getObject().getPropertyValue("c1/p1"));
    Assert.assertNotNull(hostNode.getObject().getPropertyValue("c1/p2"));
    Assert.assertNotNull(hostNode.getObject().getPropertyValue("c1/p3"));

    hostNode = tree.getChild("Host:3");
    Assert.assertEquals("Host:3", hostNode.getName());
    Assert.assertEquals(Resource.Type.Host, hostNode.getObject().getType());
    Assert.assertNotNull(hostNode.getObject().getPropertyValue("c1/p1"));
    Assert.assertNotNull(hostNode.getObject().getPropertyValue("c1/p2"));
    Assert.assertNotNull(hostNode.getObject().getPropertyValue("c1/p3"));

    hostNode = tree.getChild("Host:4");
    Assert.assertEquals("Host:4", hostNode.getName());
    Assert.assertEquals(Resource.Type.Host, hostNode.getObject().getType());
    Assert.assertNotNull(hostNode.getObject().getPropertyValue("c1/p1"));
    Assert.assertNotNull(hostNode.getObject().getPropertyValue("c1/p2"));
    Assert.assertNotNull(hostNode.getObject().getPropertyValue("c1/p3"));
  }

  private class TestQuery extends ResourceInstanceImpl {
    public TestQuery(Map<Resource.Type, String> mapIds, ResourceDefinition resourceDefinition) {
      super(mapIds, resourceDefinition,
          new ClusterControllerImpl(new ClusterControllerImplTest.TestProviderModule()));
    }
  }
}

