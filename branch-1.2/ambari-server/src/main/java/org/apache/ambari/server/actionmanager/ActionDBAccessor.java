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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.ambari.server.Role;
import org.apache.ambari.server.agent.CommandReport;

public interface ActionDBAccessor {

  public Stage getAction(String actionId);

  public List<Stage> getAllStages(long requestId);

  public void abortOperation(long requestId);

  public void timeoutHostRole(String host, long requestId, long stageId, Role role);

  /**
   * Returns all the pending stages, including queued and not-queued.
   * A stage is considered in progress if it is in progress for any host.
   */
  public List<Stage> getStagesInProgress();

  public void persistActions(List<Stage> stages);

  public void updateHostRoleState(String hostname, long requestId,
      long stageId, String role, CommandReport report);

  public void abortHostRole(String host, long requestId, long stageId,
      Role role);

  /**
   * Return the last persisted Request ID as seen when the DBAccessor object
   * was initialized.
   * Value should remain unchanged through the lifetime of the object instance.
   * @return Request Id seen at init time
   */
  public long getLastPersistedRequestIdWhenInitialized();

  /**
   * Updates scheduled stage.
   * @param s
   * @param hostname
   * @param roleStr
   */
  public void hostRoleScheduled(Stage s, String hostname, String roleStr);

  public List<HostRoleCommand> getRequestTasks(long requestId);

  public List<HostRoleCommand> getAllTasksByRequestIds(Collection<Long> requestIds);

  public List<HostRoleCommand> getTasksByRequestAndTaskIds(Collection<Long> requestIds, Collection<Long> taskIds);

  public Collection<HostRoleCommand> getTasks(Collection<Long> taskIds);

  public List<Stage> getStagesByHostRoleStatus(Set<HostRoleStatus> statuses);

  public List<Long> getRequests();

  public HostRoleCommand getTask(long taskId);

  public List<Long> getRequestsByStatus(RequestStatus status);
}
