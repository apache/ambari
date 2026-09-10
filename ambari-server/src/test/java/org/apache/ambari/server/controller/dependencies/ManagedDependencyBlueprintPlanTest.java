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
package org.apache.ambari.server.controller.dependencies;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.internal.ProvisionAction;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.state.AutoDeployInfo;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.topology.Blueprint;
import org.apache.ambari.server.topology.BlueprintValidatorImpl;
import org.apache.ambari.server.topology.Cardinality;
import org.apache.ambari.server.topology.HostGroup;
import org.apache.ambari.server.topology.Setting;
import org.junit.jupiter.api.Test;

class ManagedDependencyBlueprintPlanTest {
  @Test
  void remoteZookeeperNeverAutodeploysButExplicitLocalDaemonRetainsCardinality() {
    Blueprint blueprint = mock(Blueprint.class);
    Stack stack = mock(Stack.class);
    HostGroup group = mock(HostGroup.class);
    Set<String> names = new HashSet<>(Set.of("HBASE_MASTER", "ZOOKEEPER_CLIENT"));
    when(blueprint.getStack()).thenReturn(stack);
    when(blueprint.getServices()).thenReturn(Set.of("HBASE", "ZOOKEEPER"));
    when(blueprint.getHostGroups()).thenReturn(Map.of("consumer", group));
    when(group.getComponentNames()).thenAnswer(call -> names);
    when(blueprint.getSetting()).thenReturn(new Setting(Map.of(ManagedDependencyBlueprintPlan.SETTING,
        Set.of(new HashMap<>(Map.of("consumer_service", "HBASE", "dependency_type", "ZOOKEEPER"))))));
    when(stack.getComponents("ZOOKEEPER")).thenReturn(List.of("ZOOKEEPER_CLIENT", "ZOOKEEPER_SERVER"));
    ComponentInfo client = new ComponentInfo(); client.setName("ZOOKEEPER_CLIENT"); client.setCategory("CLIENT");
    ComponentInfo server = new ComponentInfo(); server.setName("ZOOKEEPER_SERVER"); server.setCategory("MASTER");
    when(stack.getComponentInfo("ZOOKEEPER_CLIENT")).thenReturn(client);
    when(stack.getComponentInfo("ZOOKEEPER_SERVER")).thenReturn(server);
    when(blueprint.getHostGroupsForComponent("ZOOKEEPER_SERVER")).thenAnswer(call -> names.contains("ZOOKEEPER_SERVER") ? List.of(group) : List.of());
    when(blueprint.getConfiguration()).thenReturn(new org.apache.ambari.server.topology.Configuration(Map.of(), Map.of()));
    BlueprintValidatorImpl validator = new BlueprintValidatorImpl(blueprint);
    AutoDeployInfo auto = new AutoDeployInfo(); auto.setEnabled(true); auto.setCoLocate("HBASE/HBASE_MASTER");
    assertTrue(validator.verifyComponentCardinalityCount("ZOOKEEPER_SERVER", new Cardinality("1+"), auto).isEmpty());
    assertFalse(names.contains("ZOOKEEPER_SERVER"));
    assertFalse(ManagedDependencyBlueprintPlan.externalOnlyComponent(blueprint, "ZOOKEEPER_CLIENT"));
    names.add("ZOOKEEPER_SERVER");
    assertFalse(ManagedDependencyBlueprintPlan.externalOnlyComponent(blueprint, "ZOOKEEPER_SERVER"));
    assertFalse(validator.verifyComponentCardinalityCount("ZOOKEEPER_SERVER", new Cardinality("3"), null).isEmpty());
  }

  @Test
  void prepareOnlyDoesNotScheduleInstallationOrStart() {
    var topology = mock(org.apache.ambari.server.topology.ClusterTopology.class);
    var group = mock(HostGroup.class);
    when(group.getName()).thenReturn("consumer");
    when(group.getComponentNames()).thenReturn(List.of("HBASE_MASTER", "HDFS_CLIENT", "ZOOKEEPER_CLIENT"));
    when(topology.getProvisionAction()).thenReturn(ProvisionAction.PREPARE_ONLY);
    var request = new org.apache.ambari.server.topology.HostRequest(1L, 2L, 3L,
        "consumer-host", "blueprint", group, null, topology, false);
    assertTrue(request.getLogicalTasks().isEmpty());
    assertEquals(List.of(org.apache.ambari.server.topology.tasks.TopologyTask.Type.RESOURCE_CREATION,
        org.apache.ambari.server.topology.tasks.TopologyTask.Type.CONFIGURE),
        request.getTopologyTasks().stream().map(task -> task.getType()).toList());
  }
  @Test
  void persistedRequirementsSurviveTemplateReplacementAndRequireBothBindings() throws Exception {
    var requests = mock(org.apache.ambari.server.orm.dao.TopologyRequestDAO.class);
    var dependencies = mock(org.apache.ambari.server.orm.dao.ServiceDependencyDAO.class);
    var request = new org.apache.ambari.server.orm.entities.TopologyRequestEntity();
    request.setManagedDependencyTypes("HDFS,ZOOKEEPER");
    when(requests.findProvisionByClusterId(11L)).thenReturn(request);
    var hdfs = new org.apache.ambari.server.orm.entities.ServiceDependencyBindingEntity();
    hdfs.setDependencyType("HDFS");
    when(dependencies.findByConsumer(11L, "HBASE")).thenReturn(List.of(hdfs));
    var planner = new ManagedDependencyBlueprintPlan(requests, dependencies);
    assertThrows(org.apache.ambari.server.AmbariException.class, () -> planner.requireBindings(11L));
    var zk = new org.apache.ambari.server.orm.entities.ServiceDependencyBindingEntity();
    zk.setDependencyType("ZOOKEEPER");
    when(dependencies.findByConsumer(11L, "HBASE")).thenReturn(List.of(hdfs, zk));
    assertDoesNotThrow(() -> planner.requireBindings(11L));
  }

}
