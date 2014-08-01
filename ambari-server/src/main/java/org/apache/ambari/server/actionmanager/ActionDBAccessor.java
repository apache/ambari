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
package org.apache.ambari.server.actionmanager;

import com.google.inject.persist.Transactional;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ActionDBAccessor {

  /**
   * Given an action id of the form requestId-stageId, retrieve the Stage
   */
  public Stage getStage(String actionId);

  /**
   * Get all stages associated with a single request id
   */
  public List<Stage> getAllStages(long requestId);

  /**
   * Get request object by id
   * @param requestId
   * @return
   */
  Request getRequest(long requestId);

  /**
   * Abort all outstanding operations associated with the given request
   */
  public void abortOperation(long requestId);

  /**
   * Mark the task as to have timed out
   */
  public void timeoutHostRole(String host, long requestId, long stageId, String role);

  /**
   * Returns all the pending stages, including queued and not-queued.
   * A stage is considered in progress if it is in progress for any host.
   */
  public List<Stage> getStagesInProgress();

  /**
   * Persists all tasks for a given request
   * @param request request object
   */
  @Transactional
  void persistActions(Request request) throws AmbariException;

  @Transactional
  void startRequest(long requestId);

  @Transactional
  void endRequest(long requestId);

  /**
   * Updates request with link to source schedule
   */
  @Transactional
  void setSourceScheduleForRequest(long requestId, long scheduleId);

  /**
   * Update tasks according to command reports
   */
  void updateHostRoleStates(Collection<CommandReport> reports);

  /**
   * For the given host, update all the tasks based on the command report
   */
  public void updateHostRoleState(String hostname, long requestId,
                                  long stageId, String role, CommandReport report);

  /**
   * Mark the task as to have been aborted
   */
  public void abortHostRole(String host, long requestId, long stageId, String role);

  /**
   * Mark the task as to have been aborted. Reason should be specified manually.
   */
  public void abortHostRole(String host, long requestId, long stageId,
                            String role, String reason);

  /**
   * Return the last persisted Request ID as seen when the DBAccessor object
   * was initialized.
   * Value should remain unchanged through the lifetime of the object instance.
   *
   * @return Request Id seen at init time
   */
  public long getLastPersistedRequestIdWhenInitialized();

  /**
   * Bulk update scheduled commands
   */
  void bulkHostRoleScheduled(Stage s, List<ExecutionCommand> commands);

  /**
   * Bulk abort commands
   */
  void bulkAbortHostRole(Stage s, List<ExecutionCommand> commands);

  /**
   * Updates scheduled stage.
   */
  public void hostRoleScheduled(Stage s, String hostname, String roleStr);

  /**
   * Given a request id, get all the tasks that belong to this request
   */
  public List<HostRoleCommand> getRequestTasks(long requestId);

  /**
   * Given a list of request ids, get all the tasks that belong to these requests
   */
  public List<HostRoleCommand> getAllTasksByRequestIds(Collection<Long> requestIds);

  /**
   * Get a list of host role commands where the request id belongs to the input requestIds and
   * the task id belongs to the input taskIds
   */
  public List<HostRoleCommand> getTasksByRequestAndTaskIds(Collection<Long> requestIds, Collection<Long> taskIds);

  /**
   * Given a list of task ids, get all the host role commands
   */
  public Collection<HostRoleCommand> getTasks(Collection<Long> taskIds);

  /**
   * Get all stages that contain tasks with specified host role statuses
   */
  public List<Stage> getStagesByHostRoleStatus(Set<HostRoleStatus> statuses);

  /**
   * Gets the host role command corresponding to the task id
   */
  public HostRoleCommand getTask(long taskId);

  /**
   * Get first or last maxResults requests that are in the specified status
   *
   * @param status
   *          Desired request status
   * @param maxResults
   *          maximal number of returned id's
   * @param ascOrder
   *          defines sorting order for database query result
   * @return First or last maxResults request id's if ascOrder is true or false,
   *         respectively
   */
  public List<Long> getRequestsByStatus(RequestStatus status, int maxResults, boolean ascOrder);

  /**
   * Gets request contexts associated with the list of request id
   */
  public Map<Long, String> getRequestContext(List<Long> requestIds);

  /**
   * Gets the request context associated with the request id
   */
  public String getRequestContext(long requestId);

  /**
   * Gets request objects by ids
   */
  public List<Request> getRequests(Collection<Long> requestIds);
}
