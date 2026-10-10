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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.entities.ExecutionCommandEntity;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.utils.StageUtils;
import org.junit.Test;

import com.google.inject.Provider;

public class MpackTaskUsageTest {
  @Test
  public void onlyAffectedTasksBlockAndMissingOrForeignInputsRemainUnresolved() throws Exception {
    MpackTaskUsage usage = new MpackTaskUsage();
    Cluster cluster = mock(Cluster.class);
    when(cluster.getClusterId()).thenReturn(1L); when(cluster.getClusterName()).thenReturn("primary");
    when(cluster.getDesiredStackVersion()).thenReturn(new StackId("BASE", "1.0"));
    for (String name : List.of("DATABASE", "QUEUE")) {
      Service service = mock(Service.class); when(service.getName()).thenReturn(name);
      when(service.getDesiredStackId()).thenReturn(new StackId("BASE", "1.0"));
      when(cluster.getService(name)).thenReturn(service);
    }
    Cluster other = mock(Cluster.class); when(other.getClusterId()).thenReturn(2L);
    when(other.getDesiredStackVersion()).thenReturn(new StackId("OTHER", "1.0"));
    when(other.getServices()).thenReturn(Map.of());
    Clusters clusters = mock(Clusters.class); when(clusters.getClusters()).thenReturn(Map.of("primary", cluster, "other", other));
    HostRoleCommandDAO tasks = mock(HostRoleCommandDAO.class);
    HostRoleCommandEntity missing = task(3, "DATABASE", "primary"); missing.setExecutionCommand(null);
    when(tasks.findActiveByClusters(Set.of(1L))).thenReturn(List.of(
        task(1, "DATABASE", "primary"), task(2, "QUEUE", "primary"), missing, task(4, "QUEUE", "foreign")));
    MpackActivationTest.inject(usage, "clusters", (Provider<Clusters>) () -> clusters);
    MpackActivationTest.inject(usage, "tasks", tasks);
    var blockers = usage.blockers(List.of(new MpackScope("BASE", "1.0", "DATABASE", Set.of())));
    assertEquals(List.of(1L, 3L, 4L), blockers.stream().map(value -> value.get("task_id")).toList());
    assertEquals("HOLDING", blockers.get(0).get("status"));
    assertEquals("UNRESOLVED_TASK_INPUT", blockers.get(1).get("reason"));
    assertEquals("UNRESOLVED_TASK_INPUT", blockers.get(2).get("reason"));
    verify(tasks).findActiveByClusters(Set.of(1L));
  }

  private static HostRoleCommandEntity task(long id, String service, String cluster) {
    HostRoleCommandEntity task = new HostRoleCommandEntity(); task.setTaskId(id);
    task.setRequestId(10L); task.setStatus(HostRoleStatus.HOLDING);
    StageEntity stage = new StageEntity(); stage.setClusterId(1L); stage.setRequestId(10L); stage.setStageId(1L); task.setStage(stage);
    ExecutionCommand command = new ExecutionCommand(); command.setTaskId(id); command.setClusterName(cluster); command.setServiceName(service);
    command.setCommandParams(Map.of(MpackExecutionResources.RESOURCE_REFERENCES,
        "{\"DATABASE/package\":\"mpacks/" + "a".repeat(64) + "/DATABASE/package\"}"));
    ExecutionCommandEntity input = new ExecutionCommandEntity();
    input.setCommand(StageUtils.getGson().toJson(command).getBytes(StandardCharsets.UTF_8));
    task.setExecutionCommand(input);
    return task;
  }
}
