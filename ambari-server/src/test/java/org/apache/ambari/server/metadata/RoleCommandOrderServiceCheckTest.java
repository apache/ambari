/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.metadata;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.stageplanner.RoleGraph;
import org.apache.ambari.server.stageplanner.RoleGraphNode;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.stack.StackRoleCommandOrder;
import org.junit.Test;

public class RoleCommandOrderServiceCheckTest {
  private ComponentInfo component(String name, boolean client) {
    ComponentInfo result = mock(ComponentInfo.class);
    when(result.getName()).thenReturn(name);
    when(result.isClient()).thenReturn(client);
    return result;
  }

  private RoleCommandOrder order(Map<String, Object> dependencies, String serviceName) throws Exception {
    ServiceInfo serviceInfo = mock(ServiceInfo.class);
    when(serviceInfo.getName()).thenReturn(serviceName);
    List<ComponentInfo> components = Arrays.asList(
        component("DEMO_SERVER", false), component("DEMO_AUTH", false), component("DEMO_CLIENT", true));
    when(serviceInfo.getComponents()).thenReturn(components);
    StackInfo stack = mock(StackInfo.class);
    when(stack.getServices()).thenReturn(Collections.singletonList(serviceInfo));
    when(stack.getRoleCommandOrder()).thenReturn(new StackRoleCommandOrder(
        new HashMap<>(Collections.singletonMap("general_deps", dependencies))));
    Service service = mock(Service.class);
    when(service.getDesiredStackId()).thenReturn(new StackId("TEST", "1.0"));
    Cluster cluster = mock(Cluster.class);
    when(cluster.getServices()).thenReturn(Collections.singletonMap(serviceName, service));
    RoleCommandOrder order = new RoleCommandOrder();
    order.ambariMetaInfo = mock(AmbariMetaInfo.class);
    when(order.ambariMetaInfo.getStack("TEST", "1.0")).thenReturn(stack);
    order.actionMetadata = new ActionMetadata();
    order.actionMetadata.addServiceCheckAction(serviceName);
    order.initialize(cluster, new LinkedHashSet<>());
    return order;
  }

  private RoleGraphNode node(String role, RoleCommand command) {
    return new RoleGraphNode(Role.valueOf(role), command);
  }

  private HostRoleCommand command(Role role, RoleCommand command) {
    HostRoleCommand result = mock(HostRoleCommand.class);
    when(result.getRole()).thenReturn(role);
    when(result.getRoleCommand()).thenReturn(command);
    return result;
  }

  @Test
  public void plansDaemonStartupBeforeUndeclaredServiceChecks() throws Exception {
    RoleCommandOrder order = order(Collections.emptyMap(), "DEMO");
    RoleGraph graph = new RoleGraph(order);
    HostRoleCommandFactory factory = mock(HostRoleCommandFactory.class);
    when(factory.create(anyString(), any(Role.class), isNull(), any(RoleCommand.class)))
        .thenAnswer(call -> command(call.getArgument(1), call.getArgument(3)));
    Field field = RoleGraph.class.getDeclaredField("hrcFactory");
    field.setAccessible(true);
    field.set(graph, factory);
    Map<String, HostRoleCommand> commands = new HashMap<>();
    commands.put("DEMO_SERVER", command(Role.valueOf("DEMO_SERVER"), RoleCommand.START));
    commands.put("DEMO_AUTH", command(Role.valueOf("DEMO_AUTH"), RoleCommand.START));
    commands.put("DEMO_SERVICE_CHECK", command(Role.valueOf("DEMO_SERVICE_CHECK"), RoleCommand.SERVICE_CHECK));
    List<Map<String, List<HostRoleCommand>>> stages = graph.getOrderedHostRoleCommands(Collections.singletonMap("host1", commands));
    assertEquals(2, stages.size());
    Set<String> starters = stages.get(0).get("host1").stream().map(task -> task.getRole().toString()).collect(Collectors.toSet());
    assertEquals(new HashSet<>(Arrays.asList("DEMO_SERVER", "DEMO_AUTH")), starters);
    assertEquals("DEMO_SERVICE_CHECK", stages.get(1).get("host1").get(0).getRole().toString());
  }

  @Test
  public void doesNotOrderClientsOrForeignComponentsBeforeChecks() throws Exception {
    RoleCommandOrder order = order(Collections.emptyMap(), "DEMO");
    RoleGraphNode check = node("DEMO_SERVICE_CHECK", RoleCommand.SERVICE_CHECK);
    assertEquals(0, order.order(node("DEMO_CLIENT", RoleCommand.START), check));
    assertEquals(0, order.order(node("DEMO_SERVER_OLD", RoleCommand.START), check));
  }

  @Test
  public void preservesDeclaredAndPreStartCheckOrdering() throws Exception {
    Map<String, Object> declared = new HashMap<>();
    declared.put("DEMO_SERVICE_CHECK-SERVICE_CHECK", new ArrayList<>(Collections.singletonList("DEMO_SERVER-INSTALL")));
    RoleCommandOrder order = order(declared, "DEMO");
    assertEquals(0, order.order(node("DEMO_SERVER", RoleCommand.START), node("DEMO_SERVICE_CHECK", RoleCommand.SERVICE_CHECK)));
    Map<String, Object> preStart = new HashMap<>();
    preStart.put("DEMO_SERVER-START", new ArrayList<>(Collections.singletonList("PREPARE-EXECUTE")));
    preStart.put("PREPARE-EXECUTE", new ArrayList<>(Collections.singletonList("DEMO_SERVICE_CHECK-SERVICE_CHECK")));
    order = order(preStart, "DEMO");
    assertEquals(-1, order.order(node("DEMO_SERVICE_CHECK", RoleCommand.SERVICE_CHECK), node("DEMO_SERVER", RoleCommand.START)));
    assertEquals(0, order.order(node("DEMO_AUTH", RoleCommand.START), node("DEMO_SERVICE_CHECK", RoleCommand.SERVICE_CHECK)));
  }

  @Test
  public void usesRegisteredCheckIdentityAndPreservesItInClones() throws Exception {
    RoleCommandOrder order = order(Collections.emptyMap(), "ZOOKEEPER");
    RoleCommandOrder clone = (RoleCommandOrder) order.clone();
    assertEquals(-1, clone.order(node("DEMO_SERVER", RoleCommand.START), node("ZOOKEEPER_QUORUM_SERVICE_CHECK", RoleCommand.SERVICE_CHECK)));
    assertEquals(0, clone.order(node("DEMO_SERVER", RoleCommand.START), node("ZOOKEEPER_SERVICE_CHECK", RoleCommand.SERVICE_CHECK)));
    Cluster empty = mock(Cluster.class);
    when(empty.getServices()).thenReturn(Collections.emptyMap());
    order.initialize(empty, new LinkedHashSet<>());
    assertEquals(0, order.order(node("DEMO_SERVER", RoleCommand.START), node("ZOOKEEPER_QUORUM_SERVICE_CHECK", RoleCommand.SERVICE_CHECK)));
    assertEquals(-1, clone.order(node("DEMO_SERVER", RoleCommand.START), node("ZOOKEEPER_QUORUM_SERVICE_CHECK", RoleCommand.SERVICE_CHECK)));
  }
}
