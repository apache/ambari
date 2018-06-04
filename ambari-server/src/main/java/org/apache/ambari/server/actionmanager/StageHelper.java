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

import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.ambari.server.Role;
import org.apache.ambari.server.controller.internal.RequestStageContainer;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.State;
import org.apache.commons.lang3.tuple.Pair;

public class StageHelper {

  /**
   * This method is a temporary workaround for the limitation that a stage cannot contain multiple instances of the same host-component pair.
   * It is used by stage creation to assign client components, which may have an instance for each mpack, to different stages.
   *
   * It takes a map of host components in the format that matches the service component host map of
   * {@link org.apache.ambari.server.controller.AmbariManagementControllerImpl#doStageCreation(RequestStageContainer, Cluster, Map, Map, Map, Set)},
   * and creates a list of maps of the same structure.  Each map in the list will have only a single instance of each component type.
   *
   * Example: if the original map has 2 instances of ZOOKEEPER_CLIENT, 2 of HADOOP_CLIENT and 1 of HBASE_CLIENT, the first map in the list will have 1 of each,
   * and the second map will have only 1 of ZOOKEEPER_CLIENT and 1 of HADOOP_CLIENT.
   *
   * The components are removed from the input map, which can be used to create a separate stage for the non-client components.
   */
  public static List<Map<String, Map<State, List<ServiceComponentHost>>>> deduplicateClients(
    Map<String, ? extends Map<State, ? extends List<ServiceComponentHost>>> serviceComponentHostsByHost
  ) {
    Map<Pair<String, Role>, Deque<Pair<State, ServiceComponentHost>>> clientComponents = new HashMap<>();
    for (Map.Entry<String, ? extends Map<State, ? extends List<ServiceComponentHost>>> byHost : serviceComponentHostsByHost.entrySet()) {
      for (Map.Entry<State, ? extends List<ServiceComponentHost>> byState : byHost.getValue().entrySet()) {
        for (ServiceComponentHost sch : byState.getValue()) {
          if (sch.isClientComponent()) {
            Role role = Role.valueOf(sch.getServiceComponentType());
            String hostName = byHost.getKey();
            State state = byState.getKey();
            clientComponents
              .computeIfAbsent(Pair.of(hostName, role), __ -> new LinkedList<>())
              .add(Pair.of(state, sch));
          }
        }
      }
    }

    List<Map<String, Map<State, List<ServiceComponentHost>>>> clientStages = new LinkedList<>();
    while (!clientComponents.isEmpty()) {
      Map<String, Map<State, List<ServiceComponentHost>>> stage = new HashMap<>();
      for (Iterator<Map.Entry<Pair<String, Role>, Deque<Pair<State, ServiceComponentHost>>>> iter = clientComponents.entrySet().iterator(); iter.hasNext(); ) {
        Map.Entry<Pair<String, Role>, Deque<Pair<State, ServiceComponentHost>>> entry = iter.next();
        String hostName = entry.getKey().getLeft();
        Deque<Pair<State, ServiceComponentHost>> list = entry.getValue();
        Pair<State, ServiceComponentHost> first = list.removeFirst();
        State state = first.getLeft();
        ServiceComponentHost sch = first.getRight();
        stage.computeIfAbsent(hostName, __ -> new TreeMap<>())
          .computeIfAbsent(state, __ -> new LinkedList<>())
          .add(sch);
        if (list.isEmpty()) {
          iter.remove();
        }

        Map<State, ? extends List<ServiceComponentHost>> byState = serviceComponentHostsByHost.get(hostName);
        List<ServiceComponentHost> serviceComponentHosts = byState.get(state);
        serviceComponentHosts.remove(sch);
        if (serviceComponentHosts.isEmpty()) {
          byState.remove(state);
        }
        if (byState.isEmpty()) {
          serviceComponentHostsByHost.remove(hostName);
        }
      }
      if (!stage.isEmpty()) {
        clientStages.add(stage);
      }
    }
    return clientStages;
  }
}
