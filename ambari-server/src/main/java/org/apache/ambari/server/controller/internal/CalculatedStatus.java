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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.orm.dao.HostRoleCommandStatusSummaryDTO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.StageEntity;

/**
 * Status of a request resource, calculated from a set of tasks or stages.
 */
public class CalculatedStatus {

  /**
   * The calculated overall status.
   */
  private final HostRoleStatus status;

  /**
   * The display status.
   */
  private final HostRoleStatus displayStatus;

  /**
   * The calculated percent complete.
   */
  private final double percent;

  /**
   * A status which represents a COMPLETED state at 100%
   */
  public static final CalculatedStatus COMPLETED = new CalculatedStatus(HostRoleStatus.COMPLETED,
      HostRoleStatus.COMPLETED, 100.0);

  /**
   * A status which represents a PENDING state at 0%
   */
  public static final CalculatedStatus PENDING = new CalculatedStatus(HostRoleStatus.PENDING,
      HostRoleStatus.PENDING, 0.0);

  /**
   * A status which represents an ABORTED state at -1%
   */
  public static final CalculatedStatus ABORTED = new CalculatedStatus(HostRoleStatus.ABORTED, HostRoleStatus.ABORTED, -1);

  // ----- Constructors ------------------------------------------------------

  /**
   * Constructor.
   *
   * @param status   the calculated overall status
   * @param percent  the calculated percent complete
   */
  private CalculatedStatus(HostRoleStatus status, double percent) {
    this(status, null, percent);
  }

  /**
   * Overloaded constructor that allows to set the display status if required.
   *
   * @param status   the calculated overall status
   * @param displayStatus the calculated display status
   * @param percent  the calculated percent complete
   */
  private CalculatedStatus(HostRoleStatus status, HostRoleStatus displayStatus, double percent) {
    this.status  = status;
    this.displayStatus  = displayStatus;
    this.percent = percent;
  }


  // ----- CalculatedStatus --------------------------------------------------

  /**
   * Get the calculated status.
   *
   * @return the status
   */
  public HostRoleStatus getStatus() {
    return status;
  }

  /**
   * Get the calculated display status. The display_status field is used as
   * a hint for UI. It's effective only on UpgradeGroup level.
   * We should expose it for the following states:
   * SKIPPED_FAILED
   * FAILED
   *
   * @return the display status
   */
  public HostRoleStatus getDisplayStatus() {
    return displayStatus;
  }

  /**
   * Get the calculated percent complete.
   *
   * @return the percent complete
   */
  public double getPercent() {
    return percent;
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Factory method to create a calculated status.  Calculate request status from the given
   * collection of task entities.
   *
   * @param tasks      the collection of task entities
   * @param skippable  true if a single failed status should NOT result in an overall failed status
   *
   * @return a calculated status
   */
  public static CalculatedStatus statusFromTaskEntities(Collection<HostRoleCommandEntity> tasks, boolean skippable) {

    int size = tasks.size();

    Map<HostRoleStatus, Integer> taskStatusCounts = CalculatedStatus.calculateTaskEntityStatusCounts(tasks);

    HostRoleStatus status = calculateSummaryStatusOfStage(taskStatusCounts, size, skippable);

    double progressPercent = calculateProgressPercent(taskStatusCounts, size);

    return new CalculatedStatus(status, progressPercent);
  }

  /**
   * Factory method to create a calculated status.  Calculate request status from the given
   * collection of stage entities.
   *
   * @param stages  the collection of stage entities
   *
   * @return a calculated status
   */
  public static CalculatedStatus statusFromStageEntities(Collection<StageEntity> stages) {
    Collection<HostRoleStatus> stageStatuses = new HashSet<HostRoleStatus>();
    Collection<HostRoleCommandEntity> tasks = new HashSet<HostRoleCommandEntity>();

    for (StageEntity stage : stages) {
      // get all the tasks for the stage
      Collection<HostRoleCommandEntity> stageTasks = stage.getHostRoleCommands();

      // calculate the stage status from the task status counts
      HostRoleStatus stageStatus =
          calculateSummaryStatusOfStage(calculateTaskEntityStatusCounts(stageTasks), stageTasks.size(), stage.isSkippable());

      stageStatuses.add(stageStatus);

      // keep track of all of the tasks for all stages
      tasks.addAll(stageTasks);
    }

    // calculate the overall status from the stage statuses
    HostRoleStatus status = calculateSummaryStatusOfUpgrade(calculateStatusCounts(stageStatuses), stageStatuses.size());

    // calculate the progress from the task status counts
    double progressPercent = calculateProgressPercent(calculateTaskEntityStatusCounts(tasks), tasks.size());

    return new CalculatedStatus(status, progressPercent);
  }

  /**
   * Factory method to create a calculated status.  Calculate request status from the given
   * collection of stages.
   *
   * @param stages  the collection of stages
   *
   * @return a calculated status
   */
  public static CalculatedStatus statusFromStages(Collection<Stage> stages) {

    Collection<HostRoleStatus> stageStatuses = new HashSet<HostRoleStatus>();
    Collection<HostRoleCommand> tasks = new HashSet<HostRoleCommand>();

    for (Stage stage : stages) {
      // get all the tasks for the stage
      Collection<HostRoleCommand> stageTasks = stage.getOrderedHostRoleCommands();

      // calculate the stage status from the task status counts
      HostRoleStatus stageStatus =
          calculateSummaryStatusOfStage(calculateTaskStatusCounts(stageTasks), stageTasks.size(), stage.isSkippable());

      stageStatuses.add(stageStatus);

      // keep track of all of the tasks for all stages
      tasks.addAll(stageTasks);
    }

    // calculate the overall status from the stage statuses
    HostRoleStatus status = calculateSummaryStatusOfUpgrade(calculateStatusCounts(stageStatuses), stageStatuses.size());

    // calculate the progress from the task status counts
    double progressPercent = calculateProgressPercent(calculateTaskStatusCounts(tasks), tasks.size());

    return new CalculatedStatus(status, progressPercent);
  }

  /**
   * Returns counts of tasks that are in various states.
   *
   * @param hostRoleStatuses  the collection of tasks
   *
   * @return a map of counts of tasks keyed by the task status
   */
  public static Map<HostRoleStatus, Integer> calculateStatusCounts(Collection<HostRoleStatus> hostRoleStatuses) {
    Map<HostRoleStatus, Integer> counters = new HashMap<HostRoleStatus, Integer>();
    // initialize
    for (HostRoleStatus hostRoleStatus : HostRoleStatus.values()) {
      counters.put(hostRoleStatus, 0);
    }
    // calculate counts
    for (HostRoleStatus status : hostRoleStatuses) {
      // count tasks where isCompletedState() == true as COMPLETED
      // but don't count tasks with COMPLETED status twice
      if (status.isCompletedState() && status != HostRoleStatus.COMPLETED) {
        // Increase total number of completed tasks;
        counters.put(HostRoleStatus.COMPLETED, counters.get(HostRoleStatus.COMPLETED) + 1);
      }
      // Increment counter for particular status
      counters.put(status, counters.get(status) + 1);
    }

    // We overwrite the value to have the sum converged
    counters.put(HostRoleStatus.IN_PROGRESS,
        hostRoleStatuses.size() -
            counters.get(HostRoleStatus.COMPLETED) -
            counters.get(HostRoleStatus.QUEUED) -
            counters.get(HostRoleStatus.PENDING));

    return counters;
  }

  /**
   * Returns counts of task entities that are in various states.
   *
   * @param tasks  the collection of task entities
   *
   * @return a map of counts of tasks keyed by the task status
   */
  public static Map<HostRoleStatus, Integer> calculateTaskEntityStatusCounts(Collection<HostRoleCommandEntity> tasks) {
    Collection<HostRoleStatus> hostRoleStatuses = new LinkedList<HostRoleStatus>();

    for (HostRoleCommandEntity hostRoleCommand : tasks) {
      hostRoleStatuses.add(hostRoleCommand.getStatus());
    }
    return calculateStatusCounts(hostRoleStatuses);
  }

  /**
   * Return counts of task statuses.
   * @param stageDto  the map of stage-to-summary value objects
   * @param stageIds  the stage ids to consider from the value objects
   * @return the map of status to counts
   */
  public static Map<HostRoleStatus, Integer> calculateTaskStatusCounts(
      Map<Long, HostRoleCommandStatusSummaryDTO> stageDto, Set<Long> stageIds) {

    List<HostRoleStatus> status = new ArrayList<HostRoleStatus>();

    for (Long stageId : stageIds) {
      if (!stageDto.containsKey(stageId)) {
        continue;
      }

      HostRoleCommandStatusSummaryDTO dto = stageDto.get(stageId);

      status.addAll(dto.getTaskStatuses());
    }

    return calculateStatusCounts(status);
  }

  /**
   * Calculates the overall status of an upgrade. If there are no tasks, then a
   * status of {@link HostRoleStatus#COMPLETED} is returned.
   *
   * @param stageDto
   *          the map of stage-to-summary value objects
   * @param stageIds
   *          the stage ids to consider from the value objects
   * @return the calculated status
   */
  public static CalculatedStatus statusFromStageSummary(Map<Long, HostRoleCommandStatusSummaryDTO> stageDto,
      Set<Long> stageIds) {

    // if either are empty, then we have no tasks and therefore no status - we
    // should return COMPLETED. This can happen if someone removes all tasks but
    // leaves the stages and request
    if (stageDto.isEmpty() || stageIds.isEmpty()) {
      return COMPLETED;
    }

    Collection<HostRoleStatus> stageStatuses = new HashSet<>();
    Collection<HostRoleStatus> stageDisplayStatuses = new HashSet<>();
    Collection<HostRoleStatus> taskStatuses = new ArrayList<>();

    for (Long stageId : stageIds) {
      if (!stageDto.containsKey(stageId)) {
        continue;
      }

      HostRoleCommandStatusSummaryDTO summary = stageDto.get(stageId);

      int total = summary.getTaskTotal();
      boolean skip = summary.isStageSkippable();
      Map<HostRoleStatus, Integer> counts = calculateStatusCounts(summary.getTaskStatuses());
      HostRoleStatus stageStatus = calculateSummaryStatusOfStage(counts, total, skip);
      HostRoleStatus stageDisplayStatus = calculateSummaryDisplayStatus(counts, total, skip);

      stageStatuses.add(stageStatus);
      stageDisplayStatuses.add(stageDisplayStatus);
      taskStatuses.addAll(summary.getTaskStatuses());
    }

    // calculate the overall status from the stage statuses
    Map<HostRoleStatus, Integer> counts = calculateStatusCounts(stageStatuses);
    Map<HostRoleStatus, Integer> displayCounts = calculateStatusCounts(stageDisplayStatuses);

    HostRoleStatus status = calculateSummaryStatusOfUpgrade(counts, stageStatuses.size());
    HostRoleStatus displayStatus = calculateSummaryDisplayStatus(displayCounts, stageDisplayStatuses.size(), false);

    double progressPercent = calculateProgressPercent(calculateStatusCounts(taskStatuses), taskStatuses.size());

    return new CalculatedStatus(status, displayStatus, progressPercent);
  }

  /**
   * Returns counts of tasks that are in various states.
   *
   * @param tasks  the collection of tasks
   *
   * @return a map of counts of tasks keyed by the task status
   */
  private static Map<HostRoleStatus, Integer> calculateTaskStatusCounts(Collection<HostRoleCommand> tasks) {
    Collection<HostRoleStatus> hostRoleStatuses = new LinkedList<HostRoleStatus>();

    for (HostRoleCommand hostRoleCommand : tasks) {
      hostRoleStatuses.add(hostRoleCommand.getStatus());
    }
    return calculateStatusCounts(hostRoleStatuses);
  }

  /**
   * Calculate the percent complete based on the given status counts.
   *
   * @param counters  counts of resources that are in various states
   * @param total     total number of resources in request
   *
   * @return the percent complete for the stage
   */
  private static double calculateProgressPercent(Map<HostRoleStatus, Integer> counters, double total) {
    return total == 0 ? 0 :
        ((counters.get(HostRoleStatus.QUEUED)              * 0.09 +
          counters.get(HostRoleStatus.IN_PROGRESS)         * 0.35 +
          counters.get(HostRoleStatus.HOLDING)             * 0.35 +
          counters.get(HostRoleStatus.HOLDING_FAILED)      * 0.35 +
          counters.get(HostRoleStatus.HOLDING_TIMEDOUT)    * 0.35 +
          counters.get(HostRoleStatus.COMPLETED)) / total) * 100.0;
  }

  /**
   * Calculate overall status of a stage or upgrade based on the given status counts.
   *
   * @param counters   counts of resources that are in various states
   * @param total      total number of resources in request. NOTE, completed tasks may be counted twice.
   * @param skippable  true if a single failed status should NOT result in an overall failed status return
   *
   * @return summary request status based on statuses of tasks in different states.
   */
  public static HostRoleStatus calculateSummaryStatusOfStage(Map<HostRoleStatus, Integer> counters,
      int total, boolean skippable) {

    // when there are 0 tasks, return COMPLETED
    if (total == 0) {
      return HostRoleStatus.COMPLETED;
    }

    if (counters.get(HostRoleStatus.PENDING) == total) {
      return HostRoleStatus.PENDING;
    }

    // By definition, any tasks in a future stage must be held in a PENDING status.
    if (counters.get(HostRoleStatus.HOLDING) > 0 || counters.get(HostRoleStatus.HOLDING_FAILED) > 0 || counters.get(HostRoleStatus.HOLDING_TIMEDOUT) > 0) {
      return counters.get(HostRoleStatus.HOLDING) > 0 ? HostRoleStatus.HOLDING :
      counters.get(HostRoleStatus.HOLDING_FAILED) > 0 ? HostRoleStatus.HOLDING_FAILED :
      HostRoleStatus.HOLDING_TIMEDOUT;
    }

    // Because tasks are not skippable, guaranteed to be FAILED
    if (counters.get(HostRoleStatus.FAILED) > 0 && !skippable) {
      return HostRoleStatus.FAILED;
    }

    // Because tasks are not skippable, guaranteed to be TIMEDOUT
    if (counters.get(HostRoleStatus.TIMEDOUT) > 0  && !skippable) {
      return HostRoleStatus.TIMEDOUT;
    }

    int numActiveTasks = counters.get(HostRoleStatus.PENDING) + counters.get(HostRoleStatus.QUEUED) + counters.get(HostRoleStatus.IN_PROGRESS);
    // ABORTED only if there are no other active tasks
    if (counters.get(HostRoleStatus.ABORTED) > 0 && numActiveTasks == 0) {
      return HostRoleStatus.ABORTED;
    }

    if (counters.get(HostRoleStatus.COMPLETED) == total) {
      return HostRoleStatus.COMPLETED;
    }

    return HostRoleStatus.IN_PROGRESS;
  }

  /**
   * Calculate overall status of an upgrade.
   *
   * @param counters   counts of resources that are in various states
   * @param total      total number of resources in request
   *
   * @return summary request status based on statuses of tasks in different states.
   */
  protected static HostRoleStatus calculateSummaryStatusOfUpgrade(
      Map<HostRoleStatus, Integer> counters, int total) {
    return calculateSummaryStatusOfStage(counters, total, false);
  }

  /**
   * Calculate an overall display status based on the given status counts.
   *
   * @param counters   counts of resources that are in various states
   * @param total      total number of resources in request
   * @param skippable  true if a single failed status should NOT result in an overall failed status return
   *
   * @return summary request status based on statuses of tasks in different states.
   */
  protected static HostRoleStatus calculateSummaryDisplayStatus(
      Map<HostRoleStatus, Integer> counters, int total, boolean skippable) {
    return counters.get(HostRoleStatus.SKIPPED_FAILED) > 0 ? HostRoleStatus.SKIPPED_FAILED :
           counters.get(HostRoleStatus.FAILED) > 0 ? HostRoleStatus.FAILED:
           calculateSummaryStatusOfStage(counters, total, skippable);
  }
}
