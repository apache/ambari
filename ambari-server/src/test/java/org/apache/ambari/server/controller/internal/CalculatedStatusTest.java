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
package org.apache.ambari.server.controller.internal;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * CalculatedStatus tests.
 */
@SuppressWarnings("unchecked")
public class CalculatedStatusTest {

  private Injector m_injector;

  @Inject
  HostRoleCommandFactory hostRoleCommandFactory;

  private static long taskId = 0L;
  private static long stageId = 0L;

  private static Field s_field;

  @Before()
  public void setup() throws Exception {
    m_injector = Guice.createInjector(new InMemoryDefaultTestModule());
    m_injector.getInstance(GuiceJpaInitializer.class);
    m_injector.injectMembers(this);

    s_field = HostRoleCommand.class.getDeclaredField("taskId");
    s_field.setAccessible(true);
  }


  @Test
  public void testGetStatus() throws Exception {
    Collection<HostRoleCommandEntity> tasks =
        getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED, HostRoleStatus.PENDING,
            HostRoleStatus.PENDING, HostRoleStatus.PENDING);

    CalculatedStatus status = CalculatedStatus.statusFromTaskEntities(tasks, false);

    assertEquals(HostRoleStatus.IN_PROGRESS, status.getStatus());
  }

  @Test
  public void testGetPercent() throws Exception {
    Collection<HostRoleCommandEntity> tasks =
        getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED, HostRoleStatus.PENDING,
            HostRoleStatus.PENDING, HostRoleStatus.PENDING);

    CalculatedStatus status = CalculatedStatus.statusFromTaskEntities(tasks, false);

    assertEquals(40.0, status.getPercent(), 0.1);
  }

  @Test
  public void testStatusFromTaskEntities() throws Exception {
    // Pending stage
    Collection<HostRoleCommandEntity> tasks =
        getTaskEntities(HostRoleStatus.PENDING, HostRoleStatus.PENDING, HostRoleStatus.PENDING, HostRoleStatus.PENDING);

    CalculatedStatus status = CalculatedStatus.statusFromTaskEntities(tasks, false);

    assertEquals(HostRoleStatus.PENDING, status.getStatus());
    assertEquals(0.0, status.getPercent(), 0.1);

    // failed stage
    tasks = getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.FAILED, HostRoleStatus.ABORTED,
        HostRoleStatus.ABORTED, HostRoleStatus.ABORTED);

    status = CalculatedStatus.statusFromTaskEntities(tasks, false);

    assertEquals(HostRoleStatus.FAILED, status.getStatus());
    assertEquals(100.0, status.getPercent(), 0.1);

    // failed skippable stage
    tasks = getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.FAILED, HostRoleStatus.FAILED,
        HostRoleStatus.FAILED, HostRoleStatus.FAILED);

    status = CalculatedStatus.statusFromTaskEntities(tasks, true);

    assertEquals(HostRoleStatus.COMPLETED, status.getStatus());
    assertEquals(100.0, status.getPercent(), 0.1);

    // timed out stage
    tasks = getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.TIMEDOUT,
        HostRoleStatus.TIMEDOUT, HostRoleStatus.TIMEDOUT, HostRoleStatus.TIMEDOUT);

    status = CalculatedStatus.statusFromTaskEntities(tasks, false);

    assertEquals(HostRoleStatus.TIMEDOUT, status.getStatus());
    assertEquals(100.0, status.getPercent(), 0.1);

    // timed out skippable stage
    tasks = getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.TIMEDOUT,
        HostRoleStatus.FAILED, HostRoleStatus.TIMEDOUT, HostRoleStatus.TIMEDOUT);

    status = CalculatedStatus.statusFromTaskEntities(tasks, true);

    assertEquals(HostRoleStatus.COMPLETED, status.getStatus());
    assertEquals(100.0, status.getPercent(), 0.1);

    // aborted stage
    tasks = getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.ABORTED, HostRoleStatus.ABORTED,
        HostRoleStatus.ABORTED, HostRoleStatus.ABORTED);

    status = CalculatedStatus.statusFromTaskEntities(tasks, false);

    assertEquals(HostRoleStatus.ABORTED, status.getStatus());
    assertEquals(100.0, status.getPercent(), 0.1);

    // aborted skippable stage (same as non-skippable)
    tasks = getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.ABORTED, HostRoleStatus.ABORTED,
        HostRoleStatus.ABORTED, HostRoleStatus.ABORTED);

    status = CalculatedStatus.statusFromTaskEntities(tasks, true);

    assertEquals(HostRoleStatus.ABORTED, status.getStatus());
    assertEquals(100.0, status.getPercent(), 0.1);

    // in progress stage
    tasks = getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED, HostRoleStatus.PENDING,
        HostRoleStatus.PENDING, HostRoleStatus.PENDING);

    status = CalculatedStatus.statusFromTaskEntities(tasks, false);

    assertEquals(HostRoleStatus.IN_PROGRESS, status.getStatus());
    assertEquals(40.0, status.getPercent(), 0.1);

    // completed stage
    tasks = getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED,
        HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED);

    status = CalculatedStatus.statusFromTaskEntities(tasks, false);

    assertEquals(HostRoleStatus.COMPLETED, status.getStatus());
    assertEquals(100.0, status.getPercent(), 0.1);

    // holding stage
    tasks = getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED, HostRoleStatus.HOLDING,
        HostRoleStatus.PENDING, HostRoleStatus.PENDING);

    status = CalculatedStatus.statusFromTaskEntities(tasks, false);

    assertEquals(HostRoleStatus.HOLDING, status.getStatus());
    assertEquals(54.0, status.getPercent(), 0.1);

    // holding failed stage
    tasks = getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED, HostRoleStatus.HOLDING_FAILED,
        HostRoleStatus.PENDING, HostRoleStatus.PENDING);

    status = CalculatedStatus.statusFromTaskEntities(tasks, false);

    assertEquals(HostRoleStatus.HOLDING_FAILED, status.getStatus());
    assertEquals(54.0, status.getPercent(), 0.1);

    // holding timed out stage
    tasks = getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED, HostRoleStatus.HOLDING_TIMEDOUT,
        HostRoleStatus.PENDING, HostRoleStatus.PENDING);

    status = CalculatedStatus.statusFromTaskEntities(tasks, false);

    assertEquals(HostRoleStatus.HOLDING_TIMEDOUT, status.getStatus());
    assertEquals(54.0, status.getPercent(), 0.1);
  }

  /**
   * Tests that aborted states calculate correctly. This is needed for upgrades
   * where the upgrade can be ABORTED and must not be calculated as COMPLETED.
   *
   * @throws Exception
   */
  @Test
  public void testAbortedCalculation() throws Exception {
    // a single task that is aborted
    Collection<HostRoleCommandEntity> tasks = getTaskEntities(HostRoleStatus.ABORTED);
    CalculatedStatus status = CalculatedStatus.statusFromTaskEntities(tasks, false);
    assertEquals(HostRoleStatus.ABORTED, status.getStatus());
    assertEquals(100.0, status.getPercent(), 0.1);

    // skippable
    status = CalculatedStatus.statusFromTaskEntities(tasks, true);
    assertEquals(HostRoleStatus.ABORTED, status.getStatus());
    assertEquals(100.0, status.getPercent(), 0.1);

    // a completed task and an aborted one
    tasks = getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.ABORTED);
    status = CalculatedStatus.statusFromTaskEntities(tasks, false);
    assertEquals(HostRoleStatus.ABORTED, status.getStatus());
    assertEquals(100.0, status.getPercent(), 0.1);

    // skippable
    status = CalculatedStatus.statusFromTaskEntities(tasks, true);
    assertEquals(HostRoleStatus.ABORTED, status.getStatus());
    assertEquals(100.0, status.getPercent(), 0.1);
  }

  @Test
  public void testStatusFromStageEntities() throws Exception {

    // completed request
    Collection<StageEntity> stages = getStageEntities(
        getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED),
        getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED),
        getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED)
    );

    CalculatedStatus status = CalculatedStatus.statusFromStageEntities(stages);

    assertEquals(HostRoleStatus.COMPLETED, status.getStatus());
    assertEquals(100.0, status.getPercent(), 0.1);

    // in progress request
    stages = getStageEntities(
        getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED),
        getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.IN_PROGRESS, HostRoleStatus.PENDING),
        getTaskEntities(HostRoleStatus.PENDING, HostRoleStatus.PENDING, HostRoleStatus.PENDING)
    );

    status = CalculatedStatus.statusFromStageEntities(stages);

    assertEquals(HostRoleStatus.IN_PROGRESS, status.getStatus());
    assertEquals(48.3, status.getPercent(), 0.1);

    // pending request
    stages = getStageEntities(
        getTaskEntities(HostRoleStatus.PENDING, HostRoleStatus.PENDING, HostRoleStatus.PENDING),
        getTaskEntities(HostRoleStatus.PENDING, HostRoleStatus.PENDING, HostRoleStatus.PENDING),
        getTaskEntities(HostRoleStatus.PENDING, HostRoleStatus.PENDING, HostRoleStatus.PENDING)
    );

    status = CalculatedStatus.statusFromStageEntities(stages);

    assertEquals(HostRoleStatus.PENDING, status.getStatus());
    assertEquals(0.0, status.getPercent(), 0.1);

    // failed request
    stages = getStageEntities(
        getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED),
        getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.FAILED, HostRoleStatus.PENDING),
        getTaskEntities(HostRoleStatus.PENDING, HostRoleStatus.PENDING, HostRoleStatus.PENDING)
    );

    status = CalculatedStatus.statusFromStageEntities(stages);

    assertEquals(HostRoleStatus.FAILED, status.getStatus());
    assertEquals(55.55, status.getPercent(), 0.1);

    // timed out request
    stages = getStageEntities(
        getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED),
        getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED, HostRoleStatus.TIMEDOUT),
        getTaskEntities(HostRoleStatus.PENDING, HostRoleStatus.PENDING, HostRoleStatus.PENDING)
    );

    status = CalculatedStatus.statusFromStageEntities(stages);

    assertEquals(HostRoleStatus.TIMEDOUT, status.getStatus());
    assertEquals(66.66, status.getPercent(), 0.1);

    // holding request
    stages = getStageEntities(
        getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED),
        getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED, HostRoleStatus.HOLDING),
        getTaskEntities(HostRoleStatus.PENDING, HostRoleStatus.PENDING, HostRoleStatus.PENDING),
        getTaskEntities(HostRoleStatus.PENDING, HostRoleStatus.PENDING, HostRoleStatus.PENDING)
    );

    status = CalculatedStatus.statusFromStageEntities(stages);

    assertEquals(HostRoleStatus.HOLDING, status.getStatus());
    assertEquals(47.5, status.getPercent(), 0.1);

    // holding failed request
    stages = getStageEntities(
        getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED),
        getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED, HostRoleStatus.HOLDING),
        getTaskEntities(HostRoleStatus.PENDING, HostRoleStatus.PENDING, HostRoleStatus.PENDING),
        getTaskEntities(HostRoleStatus.PENDING, HostRoleStatus.PENDING, HostRoleStatus.PENDING)
    );

    status = CalculatedStatus.statusFromStageEntities(stages);

    assertEquals(HostRoleStatus.HOLDING, status.getStatus());
    assertEquals(47.5, status.getPercent(), 0.1);

    // holding timed out request
    stages = getStageEntities(
        getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED),
        getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED, HostRoleStatus.HOLDING),
        getTaskEntities(HostRoleStatus.PENDING, HostRoleStatus.PENDING, HostRoleStatus.PENDING),
        getTaskEntities(HostRoleStatus.PENDING, HostRoleStatus.PENDING, HostRoleStatus.PENDING)
    );

    status = CalculatedStatus.statusFromStageEntities(stages);

    assertEquals(HostRoleStatus.HOLDING, status.getStatus());
    assertEquals(47.5, status.getPercent(), 0.1);
  }

  @Test
  public void testStatusFromStages() throws Exception {
    Collection<Stage> stages;
    CalculatedStatus status;

    // completed request
    stages = getStages(
        getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED),
        getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED),
        getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED)
    );

    status = CalculatedStatus.statusFromStages(stages);

    assertEquals(HostRoleStatus.COMPLETED, status.getStatus());
    assertEquals(100.0, status.getPercent(), 0.1);

    // in progress request
    stages = getStages(
        getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED),
        getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.IN_PROGRESS, HostRoleStatus.PENDING),
        getTaskEntities(HostRoleStatus.PENDING, HostRoleStatus.PENDING, HostRoleStatus.PENDING)
    );

    status = CalculatedStatus.statusFromStages(stages);

    assertEquals(HostRoleStatus.IN_PROGRESS, status.getStatus());
    assertEquals(48.3, status.getPercent(), 0.1);

    // pending request
    stages = getStages(
        getTaskEntities(HostRoleStatus.PENDING, HostRoleStatus.PENDING, HostRoleStatus.PENDING),
        getTaskEntities(HostRoleStatus.PENDING, HostRoleStatus.PENDING, HostRoleStatus.PENDING),
        getTaskEntities(HostRoleStatus.PENDING, HostRoleStatus.PENDING, HostRoleStatus.PENDING)
    );

    status = CalculatedStatus.statusFromStages(stages);

    assertEquals(HostRoleStatus.PENDING, status.getStatus());
    assertEquals(0.0, status.getPercent(), 0.1);

    // failed request
    stages = getStages(
        getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED),
        getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.FAILED, HostRoleStatus.PENDING),
        getTaskEntities(HostRoleStatus.PENDING, HostRoleStatus.PENDING, HostRoleStatus.PENDING)
    );

    status = CalculatedStatus.statusFromStages(stages);

    assertEquals(HostRoleStatus.FAILED, status.getStatus());
    assertEquals(55.55, status.getPercent(), 0.1);

    // timed out request
    stages = getStages(
        getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED),
        getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED, HostRoleStatus.TIMEDOUT),
        getTaskEntities(HostRoleStatus.PENDING, HostRoleStatus.PENDING, HostRoleStatus.PENDING)
    );

    status = CalculatedStatus.statusFromStages(stages);

    assertEquals(HostRoleStatus.TIMEDOUT, status.getStatus());
    assertEquals(66.66, status.getPercent(), 0.1);

    // holding request
    stages = getStages(
        getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED),
        getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED, HostRoleStatus.HOLDING),
        getTaskEntities(HostRoleStatus.PENDING, HostRoleStatus.PENDING, HostRoleStatus.PENDING),
        getTaskEntities(HostRoleStatus.PENDING, HostRoleStatus.PENDING, HostRoleStatus.PENDING)
    );

    status = CalculatedStatus.statusFromStages(stages);

    assertEquals(HostRoleStatus.HOLDING, status.getStatus());
    assertEquals(47.5, status.getPercent(), 0.1);

    // holding failed request
    stages = getStages(
        getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED),
        getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED, HostRoleStatus.HOLDING),
        getTaskEntities(HostRoleStatus.PENDING, HostRoleStatus.PENDING, HostRoleStatus.PENDING),
        getTaskEntities(HostRoleStatus.PENDING, HostRoleStatus.PENDING, HostRoleStatus.PENDING)
    );

    status = CalculatedStatus.statusFromStages(stages);

    assertEquals(HostRoleStatus.HOLDING, status.getStatus());
    assertEquals(47.5, status.getPercent(), 0.1);

    // holding timed out request
    stages = getStages(
        getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED),
        getTaskEntities(HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED, HostRoleStatus.HOLDING),
        getTaskEntities(HostRoleStatus.PENDING, HostRoleStatus.PENDING, HostRoleStatus.PENDING),
        getTaskEntities(HostRoleStatus.PENDING, HostRoleStatus.PENDING, HostRoleStatus.PENDING)
    );

    status = CalculatedStatus.statusFromStages(stages);

    assertEquals(HostRoleStatus.HOLDING, status.getStatus());
    assertEquals(47.5, status.getPercent(), 0.1);
  }

  @Test
  public void testCalculateStatusCounts() throws Exception {
    Collection<HostRoleStatus> hostRoleStatuses = new LinkedList<HostRoleStatus>();

    hostRoleStatuses.add(HostRoleStatus.PENDING);
    hostRoleStatuses.add(HostRoleStatus.QUEUED);
    hostRoleStatuses.add(HostRoleStatus.HOLDING);
    hostRoleStatuses.add(HostRoleStatus.HOLDING_FAILED);
    hostRoleStatuses.add(HostRoleStatus.HOLDING_TIMEDOUT);
    hostRoleStatuses.add(HostRoleStatus.IN_PROGRESS);
    hostRoleStatuses.add(HostRoleStatus.IN_PROGRESS);
    hostRoleStatuses.add(HostRoleStatus.COMPLETED);
    hostRoleStatuses.add(HostRoleStatus.COMPLETED);
    hostRoleStatuses.add(HostRoleStatus.COMPLETED);
    hostRoleStatuses.add(HostRoleStatus.COMPLETED);
    hostRoleStatuses.add(HostRoleStatus.FAILED);
    hostRoleStatuses.add(HostRoleStatus.TIMEDOUT);
    hostRoleStatuses.add(HostRoleStatus.ABORTED);

    Map<HostRoleStatus, Integer> counts = CalculatedStatus.calculateStatusCounts(hostRoleStatuses);

    assertEquals(1L, (long) counts.get(HostRoleStatus.PENDING));
    assertEquals(1L, (long) counts.get(HostRoleStatus.QUEUED));
    assertEquals(1L, (long) counts.get(HostRoleStatus.HOLDING));
    assertEquals(1L, (long) counts.get(HostRoleStatus.HOLDING_FAILED));
    assertEquals(1L, (long) counts.get(HostRoleStatus.HOLDING_TIMEDOUT));
    assertEquals(5L, (long) counts.get(HostRoleStatus.IN_PROGRESS));
    assertEquals(7L, (long) counts.get(HostRoleStatus.COMPLETED));
    assertEquals(1L, (long) counts.get(HostRoleStatus.FAILED));
    assertEquals(1L, (long) counts.get(HostRoleStatus.TIMEDOUT));
    assertEquals(1L, (long) counts.get(HostRoleStatus.ABORTED));
  }

  @Test
  public void testCountsWithRepeatHosts() throws Exception {
    List<Stage> stages = new ArrayList<Stage>();

      stages.addAll(getStages(getTaskEntities(
          HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED,
          HostRoleStatus.COMPLETED, HostRoleStatus.COMPLETED)));

    // create 5th stage that is a repeat of an earlier one
    HostRoleCommandEntity entity = new HostRoleCommandEntity();
    entity.setTaskId(taskId++);
    entity.setStatus(HostRoleStatus.PENDING);
    stages.addAll(getStages(Collections.singleton(entity)));

    CalculatedStatus calc = CalculatedStatus.statusFromStages(stages);

    assertEquals(HostRoleStatus.IN_PROGRESS, calc.getStatus());
    assertEquals(80d, calc.getPercent(), 0.1d);
  }


  private Collection<HostRoleCommandEntity> getTaskEntities(HostRoleStatus... statuses) {
    Collection<HostRoleCommandEntity> entities = new LinkedList<HostRoleCommandEntity>();

    for (int i = 0; i < statuses.length; i++) {
      HostRoleStatus status = statuses[i];
      HostRoleCommandEntity entity = new HostRoleCommandEntity();
      entity.setTaskId(taskId++);
      entity.setStatus(status);

      entities.add(entity);
    }
    return entities;
  }

  private Collection<StageEntity> getStageEntities(Collection<HostRoleCommandEntity> ... taskCollections) {

    Collection<StageEntity> entities = new LinkedList<StageEntity>();

    for (Collection<HostRoleCommandEntity> taskEntities : taskCollections) {
      StageEntity entity = new StageEntity();
      entity.setStageId(stageId++);
      entity.setHostRoleCommands(taskEntities);

      entities.add(entity);
    }
    return entities;
  }

  private Collection<Stage> getStages(Collection<HostRoleCommandEntity> ... taskCollections) {

    Collection<Stage> entities = new LinkedList<Stage>();

    for (Collection<HostRoleCommandEntity> taskEntities : taskCollections) {
      TestStage stage = new TestStage();
      stage.setStageId(stageId++);
      stage.setHostRoleCommands(taskEntities);

      entities.add(stage);
    }
    return entities;
  }

  private class TestStage extends Stage {

    private final List<HostRoleCommand> hostRoleCommands = new LinkedList<HostRoleCommand>();

    private TestStage() {
      super(1L, "", "", 1L, "", "", "", "", hostRoleCommandFactory);
    }

    void setHostRoleCommands(Collection<HostRoleCommandEntity> tasks) {
      for (HostRoleCommandEntity task : tasks) {
        HostRoleCommand command = HostRoleCommandHelper.createWithTaskId(task.getHostName(), taskId++, hostRoleCommandFactory);
        command.setStatus(task.getStatus());
        hostRoleCommands.add(command);
      }
    }

    @Override
    public List<HostRoleCommand> getOrderedHostRoleCommands() {
      return hostRoleCommands;
    }
  }

  private static class HostRoleCommandHelper  {

    public static HostRoleCommand createWithTaskId(String hostName, long taskId, HostRoleCommandFactory hostRoleCommandFactory1) {
      HostRoleCommand hrc = hostRoleCommandFactory1.create(hostName, Role.AMBARI_SERVER_ACTION, null, RoleCommand.START);
      try {
        s_field.set(hrc, Long.valueOf(taskId));
      } catch (Exception e) {
        e.printStackTrace();
      }
      return hrc;
    }
  }
}