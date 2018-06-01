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
package org.apache.ambari.server.actionmanager;

import static java.util.stream.Collectors.toSet;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.ambari.server.Role;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.State;
import org.easymock.EasyMockSupport;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class StageHelperTest extends EasyMockSupport {

  @Test
  public void duplicateClientsExtractedToSeparateStage() {
    String hostName1 = "c6401";
    String hostName2 = "c6402";

    Map<String, Map<State, List<ServiceComponentHost>>> serviceComponentHostsByHost = new TreeMap<>(ImmutableMap.of(
      hostName1, new TreeMap<>(ImmutableMap.of(
        State.INSTALLED, components(Role.HDFS_CLIENT, Role.ZOOKEEPER_CLIENT, Role.HDFS_CLIENT, Role.HIVE_SERVER),
        State.STARTED, components(Role.NAMENODE, Role.ZOOKEEPER_SERVER),
        State.INIT, components()
      )),
      hostName2, new TreeMap<>(ImmutableMap.of(
        State.INSTALLED, components(Role.ZOOKEEPER_CLIENT, Role.ZOOKEEPER_CLIENT),
        State.STARTED, components(Role.SECONDARY_NAMENODE),
        State.INIT, components(Role.HBASE_CLIENT, Role.HDFS_CLIENT)
      ))
    ));

    replayAll();

    List<Map<String, Map<State, List<ServiceComponentHost>>>> clients = StageHelper.deduplicateClients(serviceComponentHostsByHost);
    assertEquals(2, clients.size());
    assertEquals(ImmutableSet.of(Role.HDFS_CLIENT, Role.ZOOKEEPER_CLIENT), toRoles(clients.get(0).get(hostName1).get(State.INSTALLED)));
    assertNull(clients.get(0).get(hostName1).get(State.STARTED));
    assertNull(clients.get(0).get(hostName1).get(State.INIT));
    assertEquals(ImmutableSet.of(Role.ZOOKEEPER_CLIENT), toRoles(clients.get(0).get(hostName2).get(State.INSTALLED)));
    assertNull(clients.get(0).get(hostName2).get(State.STARTED));
    assertEquals(ImmutableSet.of(Role.HBASE_CLIENT, Role.HDFS_CLIENT), toRoles(clients.get(0).get(hostName2).get(State.INIT)));

    assertEquals(ImmutableSet.of(Role.HDFS_CLIENT), toRoles(clients.get(1).get(hostName1).get(State.INSTALLED)));
    assertEquals(ImmutableSet.of(Role.ZOOKEEPER_CLIENT), toRoles(clients.get(1).get(hostName2).get(State.INSTALLED)));

    // make sure clients are no longer present
    assertEquals(ImmutableSet.of(), serviceComponentHostsByHost.values().stream()
      .flatMap(each -> each.values().stream())
      .flatMap(Collection::stream)
      .filter(ServiceComponentHost::isClientComponent)
      .collect(toSet())
    );
  }

  private static Set<Role> toRoles(Collection<ServiceComponentHost> serviceComponentHosts) {
    return serviceComponentHosts.stream()
      .map(ServiceComponentHost::getServiceComponentType)
      .map(Role::valueOf)
      .collect(toSet());
  }

  private List<ServiceComponentHost> components(Role... roles) {
    List<ServiceComponentHost> components = new LinkedList<>();
    for (Role role : roles) {
      ServiceComponentHost sch = createNiceMock(ServiceComponentHost.class);
      expect(sch.isClientComponent()).andReturn(role.name().endsWith("_CLIENT")).anyTimes();
      expect(sch.getServiceComponentType()).andReturn(role.name()).anyTimes();
      components.add(sch);
    }
    return components;
  }

}
