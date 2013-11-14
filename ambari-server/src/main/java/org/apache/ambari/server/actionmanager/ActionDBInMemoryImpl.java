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

import com.google.inject.Singleton;
import org.apache.ambari.server.agent.CommandReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Singleton
public class ActionDBInMemoryImpl implements ActionDBAccessor {

  private static Logger LOG = LoggerFactory.getLogger(ActionDBInMemoryImpl.class);
  // for a persisted DB, this will be initialized in the ctor
  // with the highest persisted requestId value in the DB
  private final long lastRequestId = 0;
  List<Stage> stageList = new ArrayList<Stage>();

  @Override
  public synchronized Stage getStage(String actionId) {
    for (Stage s : stageList) {
      if (s.getActionId().equals(actionId)) {
        return s;
      }
    }
    return null;
  }

  @Override
  public synchronized List<Stage> getAllStages(long requestId) {
    List<Stage> l = new ArrayList<Stage>();
    for (Stage s : stageList) {
      if (s.getRequestId() == requestId) {
        l.add(s);
      }
    }
    return l;
  }

  @Override
  public synchronized void abortOperation(long requestId) {
    for (Stage s : stageList) {
      if (s.getRequestId() == requestId) {
        for (String host : s.getHostRoleCommands().keySet()) {
          Map<String, HostRoleCommand> roleCommands = s.getHostRoleCommands().get(host);
          for (String role : roleCommands.keySet()) {
            HostRoleCommand cmd = roleCommands.get(role);
            HostRoleStatus status = s.getHostRoleStatus(host, cmd.getRole()
                .toString());
            if (status.equals(HostRoleStatus.IN_PROGRESS)
                || status.equals(HostRoleStatus.QUEUED)
                || status.equals(HostRoleStatus.PENDING)) {
              s.setHostRoleStatus(host, cmd.getRole().toString(),
                  HostRoleStatus.ABORTED);
            }
          }
        }
      }
    }
  }

  @Override
  public synchronized void timeoutHostRole(String host, long requestId,
                                           long stageId, String role) {
    for (Stage s : stageList) {
      s.setHostRoleStatus(host, role.toString(), HostRoleStatus.TIMEDOUT);
    }
  }

  @Override
  public synchronized List<Stage> getStagesInProgress() {
    List<Stage> l = new ArrayList<Stage>();
    for (Stage s : stageList) {
      if (s.isStageInProgress()) {
        l.add(s);
      }
    }
    return l;
  }

  @Override
  public synchronized void persistActions(List<Stage> stages) {
    for (Stage s : stages) {
      stageList.add(s);
    }
  }

  @Override
  public synchronized void updateHostRoleState(String hostname, long requestId,
                                               long stageId, String role, CommandReport report) {
    LOG.info("DEBUG stages to iterate: " + stageList.size());
    if (null == report.getStatus()
        || null == report.getStdOut()
        || null == report.getStdErr()) {
      throw new RuntimeException("Badly formed command report.");
    }
    for (Stage s : stageList) {
      if (s.getRequestId() == requestId && s.getStageId() == stageId) {
        s.setHostRoleStatus(hostname, role,
            HostRoleStatus.valueOf(report.getStatus()));
        s.setExitCode(hostname, role, report.getExitCode());
        s.setStderr(hostname, role, report.getStdErr());
        s.setStdout(hostname, role, report.getStdOut());
      }
    }
  }

  @Override
  public void abortHostRole(String host, long requestId, long stageId, String role) {
    CommandReport report = new CommandReport();
    report.setExitCode(999);
    report.setStdErr("Host Role in invalid state");
    report.setStdOut("");
    report.setStatus("ABORTED");
    updateHostRoleState(host, requestId, stageId, role, report);
  }

  @Override
  public synchronized long getLastPersistedRequestIdWhenInitialized() {
    return lastRequestId;
  }

  @Override
  public void hostRoleScheduled(Stage s, String hostname, String roleStr) {
    //Nothing needed for in-memory implementation
  }

  @Override
  public List<HostRoleCommand> getRequestTasks(long requestId) {
    return null;
  }

  @Override
  public List<HostRoleCommand> getAllTasksByRequestIds(Collection<Long> requestIds) {
    //TODO not implemented
    return null;
  }

  @Override
  public List<HostRoleCommand> getTasksByRequestAndTaskIds(Collection<Long> requestIds, Collection<Long> taskIds) {
    //TODO not implemented
    return null;
  }

  @Override
  public Collection<HostRoleCommand> getTasks(Collection<Long> taskIds) {
    return null;
  }

  @Override
  public List<Stage> getStagesByHostRoleStatus(Set<HostRoleStatus> statuses) {
    List<Stage> l = new ArrayList<Stage>();
    for (Stage s : stageList) {
      if (s.doesStageHaveHostRoleStatus(statuses)) {
        l.add(s);
      }
    }
    return l;
  }

  @Override
  public synchronized List<Long> getRequests() {
    Set<Long> requestIds = new HashSet<Long>();
    for (Stage s : stageList) {
      requestIds.add(s.getRequestId());
    }
    List<Long> ids = new ArrayList<Long>();
    ids.addAll(requestIds);
    return ids;
  }

  public HostRoleCommand getTask(long taskId) {
    for (Stage s : stageList) {
      for (String host : s.getHostRoleCommands().keySet()) {
        Map<String, HostRoleCommand> map = s.getHostRoleCommands().get(host);
        for (HostRoleCommand hostRoleCommand : map.values()) {
          if (hostRoleCommand.getTaskId() == taskId) {
            return hostRoleCommand;
          }
        }
      }
    }
    return null;
  }

  @Override
  public List<Long> getRequestsByStatus(RequestStatus status) {
    // TODO
    throw new RuntimeException("Functionality not implemented");
  }

  @Override
  public Map<Long, String> getRequestContext(List<Long> requestIds) {
    Map<Long, String> result = new HashMap<Long, String>();
    for (Long requestId : requestIds) {
      List<Stage> stages = getAllStages(requestId);
      result.put(requestId, stages != null && !stages.isEmpty() ? stages.get
          (0).getRequestContext() : "");
    }
    return result;
  }

  @Override
  public String getRequestContext(long requestId) {
    List<Stage> stages = getAllStages(requestId);
    return stages != null && !stages.isEmpty() ? stages.get(0)
        .getRequestContext() : "";
  }
}
