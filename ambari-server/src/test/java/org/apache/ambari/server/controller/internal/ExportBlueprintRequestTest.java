/*
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

package org.apache.ambari.server.controller.internal;

import static java.util.stream.Collectors.toMap;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.api.util.TreeNodeImpl;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.topology.Blueprint;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.HostGroup;
import org.apache.ambari.server.topology.HostGroupInfo;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

/**
 * ExportBlueprintRequest unit tests.
 */
public class ExportBlueprintRequestTest {
  private static final String CLUSTER_NAME = "c1";
  private static final String CLUSTER_ID = "2";

  @Test
  public void testExport_noConfigs() throws Exception {
    AmbariManagementController controller = createNiceMock(AmbariManagementController.class);
    expect(controller.getStackServices(anyObject())).andReturn(Collections.emptySet()).anyTimes();
    expect(controller.getStackLevelConfigurations(anyObject())).andReturn(Collections.emptySet()).anyTimes();
    replay(controller);

    Resource clusterResource = new ResourceImpl(Resource.Type.Cluster);
    clusterResource.setProperty(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID, CLUSTER_NAME);
    clusterResource.setProperty(ClusterResourceProvider.CLUSTER_ID_PROPERTY_ID, CLUSTER_ID);
    clusterResource.setProperty(ClusterResourceProvider.CLUSTER_VERSION_PROPERTY_ID, "TEST-1.0");

    TreeNode<Resource> clusterNode = new TreeNodeImpl<>(null, clusterResource, "cluster");
    // add empty config child resource
    Resource configResource = new ResourceImpl(Resource.Type.Configuration);
    clusterNode.addChild(configResource, "configurations");

    Resource hostsResource = new ResourceImpl(Resource.Type.Host);
    Resource host1Resource = new ResourceImpl(Resource.Type.Host);
    Resource host2Resource = new ResourceImpl(Resource.Type.Host);
    Resource host3Resource = new ResourceImpl(Resource.Type.Host);

    TreeNode<Resource> hostsNode = clusterNode.addChild(hostsResource, "hosts");
    TreeNode<Resource> host1Node = hostsNode.addChild(host1Resource, "host_1");
    TreeNode<Resource> host2Node = hostsNode.addChild(host2Resource, "host_2");
    TreeNode<Resource> host3Node = hostsNode.addChild(host3Resource, "host_3");

    host1Resource.setProperty("Hosts/host_name", "host1");
    host2Resource.setProperty("Hosts/host_name", "host2");
    host3Resource.setProperty("Hosts/host_name", "host3");

    List<String> host1ComponentsList = Arrays.asList("NAMENODE", "HDFS_CLIENT", "ZOOKEEPER_SERVER", "SECONDARY_NAMENODE");
    List<String> host2ComponentsList = Arrays.asList("DATANODE", "HDFS_CLIENT", "ZOOKEEPER_SERVER");
    List<String> host3ComponentsList = Arrays.asList("DATANODE", "HDFS_CLIENT", "ZOOKEEPER_SERVER");

    processHostGroupComponents(host1Node, host1ComponentsList);
    processHostGroupComponents(host2Node, host2ComponentsList);
    processHostGroupComponents(host3Node, host3ComponentsList);

    // test
    ExportBlueprintRequest exportBlueprintRequest = new ExportBlueprintRequest(clusterNode, BlueprintExportType.FULL, controller);

    // assertions
    assertEquals(CLUSTER_NAME, exportBlueprintRequest.getClusterName());
    Blueprint bp = exportBlueprintRequest.getBlueprint();
    assertEquals("exported-blueprint", bp.getName());
    Map<String, HostGroup> hostGroups = bp.getHostGroups();
    assertEquals(2, hostGroups.size());
    String hg1Name = null;
    String hg2Name = null;
    for (HostGroup group : hostGroups.values()) {
      Collection<String> components = group.getComponentNames();
      if (components.containsAll(host1ComponentsList)) {
        assertEquals(host1ComponentsList.size(), components.size());
        assertEquals("1", group.getCardinality());
        hg1Name = group.getName();
      } else if (components.containsAll(host2ComponentsList)) {
        assertEquals(host2ComponentsList.size(), components.size());
        assertEquals("2", group.getCardinality());
        hg2Name = group.getName();
      } else {
        fail("Host group contained invalid components");
      }
    }

    assertNotNull(hg1Name);
    assertNotNull(hg2Name);

    HostGroupInfo host1Info = exportBlueprintRequest.getHostGroupInfo().get(hg1Name);
    assertEquals(1, host1Info.getHostNames().size());
    assertEquals("host1", host1Info.getHostNames().iterator().next());

    HostGroupInfo host2Info = exportBlueprintRequest.getHostGroupInfo().get(hg2Name);
    assertEquals(2, host2Info.getHostNames().size());
    assertTrue(host2Info.getHostNames().contains("host2") && host2Info.getHostNames().contains("host3"));
  }

  private void processHostGroupComponents(TreeNode<Resource> hostNode, Collection<String> components) {
    Resource hostComponentsResource = new ResourceImpl(Resource.Type.HostComponent);
    TreeNode<Resource> hostComponentsNode = hostNode.addChild(hostComponentsResource, "host_components");
    int componentCount = 1;
    for (String component : components) {
      Resource componentResource = new ResourceImpl(Resource.Type.HostComponent);
      componentResource.setProperty("HostRoles/component_name", component);
      hostComponentsNode.addChild(componentResource, "host_component_" + componentCount++);
    }
  }

  @Test
  public void defaultConfigs() {
    Configuration stackConfig = createTestStack();
    Configuration clusterConfig = stackConfig.copy();
    Configuration customConfig = Configuration.newEmpty();

    Stack stack = createNiceMock(Stack.class);
    expect(stack.getServices()).andReturn(ImmutableList.of("HDFS")).anyTimes();
    expect(stack.getConfiguration()).andReturn(stackConfig).anyTimes();
    replay(stack);

    TreeNode<Resource> cluster = createClusterTree(stackConfig);
    assertEquals(clusterConfig.getProperties(), ExportBlueprintRequest.createConfiguration(cluster, stack, BlueprintExportType.FULL).getProperties());
    assertEquals(customConfig.getProperties(), ExportBlueprintRequest.createConfiguration(cluster, stack, BlueprintExportType.MINIMAL).getProperties());
  }

  @Test
  public void customConfigs() {
    Configuration stackConfig = createTestStack();
    Configuration clusterConfig = stackConfig.copy();
    Configuration customConfig = Configuration.newEmpty();
    customize(clusterConfig, customConfig, "core-site", "hadoop.security.authorization", "true");
    customize(clusterConfig, customConfig, "core-site", "fs.trash.interval", "0");
    customize(clusterConfig, customConfig, "hdfs-site", "dfs.webhdfs.enabled",  "false");

    Stack stack = createNiceMock(Stack.class);
    expect(stack.getServices()).andReturn(ImmutableList.of("HDFS")).anyTimes();
    expect(stack.getConfiguration()).andReturn(stackConfig).anyTimes();
    replay(stack);

    TreeNode<Resource> cluster = createClusterTree(clusterConfig);
    assertEquals(clusterConfig.getProperties(), ExportBlueprintRequest.createConfiguration(cluster, stack, BlueprintExportType.FULL).getProperties());
    assertEquals(customConfig.getProperties(), ExportBlueprintRequest.createConfiguration(cluster, stack, BlueprintExportType.MINIMAL).getProperties());
  }

  private static Configuration createTestStack() {
    Configuration stackConfig = Configuration.newEmpty();
    stackConfig.setProperty("core-site", "io.file.buffer.size",  "131072");
    stackConfig.setProperty("core-site", "hadoop.security.authorization",  "false");
    stackConfig.setProperty("core-site", "fs.trash.interval",  "360");
    stackConfig.setProperty("hdfs-site", "dfs.namenode.name.dir",  "/hadoop/hdfs/namenode");
    stackConfig.setProperty("hdfs-site", "dfs.datanode.data.dir",  "/hadoop/hdfs/data");
    stackConfig.setProperty("hdfs-site", "dfs.webhdfs.enabled",  "true");
    return stackConfig;
  }

  private static void customize(Configuration clusterConfig, Configuration customConfig, String configType, String propertyName, String value) {
    clusterConfig.setProperty(configType, propertyName, value);
    customConfig.setProperty(configType, propertyName, value);
  }

  private static TreeNode<Resource> createClusterTree(Configuration configuration) {
    TreeNode<Resource> clusterTree = new TreeNodeImpl<>(null, new ResourceImpl(Resource.Type.Cluster), "cluster");

    DesiredConfig desiredConfig = new DesiredConfig();
    desiredConfig.setTag("TOPOLOGY_RESOLVED");
    Map<String, Object> desiredConfigs = configuration.getProperties().keySet().stream()
      .collect(toMap(Function.identity(), __ -> desiredConfig));
    clusterTree.getObject().getPropertiesMap().put("Clusters/desired_configs", desiredConfigs);

    TreeNode<Resource> configsNode = clusterTree.addChild(null, "configurations");
    configsNode.setProperty("isCollection", "true");

    for (Map.Entry<String, Map<String, String>> e : configuration.getProperties().entrySet()) {
      String configType = e.getKey();
      TreeNode<Resource> configNode = configsNode.addChild(new ResourceImpl(Resource.Type.Configuration), configType);
      configNode.getObject().setProperty("type", configType);
      configNode.getObject().setProperty("tag", "TOPOLOGY_RESOLVED");
      configNode.getObject().getPropertiesMap().put("properties", Maps.newHashMap(e.getValue()));
    }

    return clusterTree;
  }

}
