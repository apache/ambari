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
package org.apache.ambari.server.mpack;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.utils.StageUtils;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/** Task blockers are determined from persistent task identity and typed command inputs. */
@Singleton
public class MpackTaskUsage {
  @Inject private Provider<Clusters> clusters;
  @Inject private HostRoleCommandDAO tasks;

  @org.apache.ambari.server.orm.RequiresSession
  public List<Map<String, Object>> blockers(List<MpackScope> scopes) {
    if (scopes.isEmpty()) return List.of();
    Map<Long, Cluster> affected = new TreeMap<>();
    clusters.get().getClusters().values().forEach(cluster -> {
      if (scopes.stream().anyMatch(scope -> scope.contains(cluster.getDesiredStackVersion(), null)
          || cluster.getServices().values().stream().anyMatch(service -> scope.contains(service.getDesiredStackId(), service.getName())))) {
        affected.put(cluster.getClusterId(), cluster);
      }
    });
    List<Map<String, Object>> result = new ArrayList<>();
    for (HostRoleCommandEntity task : tasks.findActiveByClusters(affected.keySet())) {
      Cluster cluster = affected.get(task.getStage().getClusterId());
      String service = null;
      boolean unresolved = false;
      try {
        if (task.getExecutionCommand() == null || task.getExecutionCommand().getCommand() == null) {
          unresolved = true;
        } else {
          ExecutionCommand command = StageUtils.getGson().fromJson(
              new String(task.getExecutionCommand().getCommand(), StandardCharsets.UTF_8), ExecutionCommand.class);
          service = command.getServiceName();
          unresolved = command.getTaskId() != task.getTaskId()
              || command.getClusterName() == null || !command.getClusterName().equals(cluster.getClusterName());
        }
        org.apache.ambari.server.state.StackId stack = service == null || unresolved
            ? cluster.getDesiredStackVersion() : cluster.getService(service).getDesiredStackId();
        final String selected = unresolved ? null : service;
        if (!scopes.stream().anyMatch(scope -> scope.contains(stack, selected))) continue;
      } catch (org.apache.ambari.server.AmbariException | RuntimeException invalid) {
        unresolved = true;
      }
      result.add(Map.of("cluster_id", cluster.getClusterId(), "cluster_name", cluster.getClusterName(), "request_id", task.getRequestId(),
          "task_id", task.getTaskId(), "status", task.getStatus().name(),
          "service", service == null ? "" : service,
          "reason", unresolved ? "UNRESOLVED_TASK_INPUT" : "AFFECTED_DEFINITION_TASK"));
    }
    return List.copyOf(result);
  }
}
