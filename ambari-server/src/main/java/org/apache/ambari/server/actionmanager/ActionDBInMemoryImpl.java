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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ambari.server.Role;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.mortbay.log.Log;

import com.google.inject.Singleton;

@Singleton
public class ActionDBInMemoryImpl implements ActionDBAccessor {

  // for a persisted DB, this will be initialized in the ctor
  // with the highest persisted requestId value in the DB
  private final long lastRequestId = 0;

  List<Stage> stageList = new ArrayList<Stage>();

  @Override
  public synchronized Stage getAction(String actionId) {
    for (Stage s: stageList) {
      if (s.getActionId().equals(actionId)) {
        return s;
      }
    }
    return null;
  }
  @Override
  public synchronized List<Stage> getAllStages(long requestId) {
    List<Stage> l = new ArrayList<Stage>();
    for (Stage s: stageList) {
      System.err.println(s.getRequestId());
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
        for (String host : s.getHosts()) {
          for (ExecutionCommand cmd : s.getExecutionCommands(host)) {
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
      long stageId, Role role) {
    for (Stage s : stageList) {
      s.setHostRoleStatus(host, role.toString(), HostRoleStatus.TIMEDOUT);
    }
  }

  @Override
  public synchronized List<Stage> getStagesInProgress() {
    List<Stage> l = new ArrayList<Stage>();
    for (Stage s: stageList) {
      if (s.isStageInProgress()) {
        l.add(s);
      }
    }
    return l;
  }

  @Override
  public synchronized void persistActions(List<Stage> stages) {
    for (Stage s: stages) {
      System.err.println("adding stage "+s.getRequestId()+" "+s.getStageId());
      stageList.add(s);
    }
  }
  @Override
  public synchronized void updateHostRoleState(String hostname, long requestId,
      long stageId, String role, CommandReport report) {
    Log.info("DEBUG stages to iterate: "+stageList.size());
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
  public void abortHostRole(String host, long requestId, long stageId, Role role) {
    CommandReport report = new CommandReport();
    report.setExitCode(999);
    report.setStdErr("Host Role in invalid state");
    report.setStdOut("");
    report.setStatus("ABORTED");
    updateHostRoleState(host, requestId, stageId, role.toString(), report);
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
  public Collection<HostRoleCommand> getTasks(Collection<Long> taskIds) {
    return null;
  }
  
  @Override
  public List<Stage> getStagesByHostRoleStatus(Set<HostRoleStatus> statuses) {
    List<Stage> l = new ArrayList<Stage>();
    for (Stage s: stageList) {
      if (s.doesStageHaveHostRoleStatus(statuses)) {
        l.add(s);
      }
    }
    return l;
  }
  @Override
  public synchronized List<Long> getRequests() {
    Set<Long> requestIds = new HashSet<Long>();
    for (Stage s: stageList) {
      requestIds.add(s.getRequestId());
    }
    List<Long> ids = new ArrayList<Long>();
    ids.addAll(requestIds);
    return ids;
  }

  public HostRoleCommand getTask(long taskId) {
    for (Stage s : stageList) {
      for (String host : s.getHosts()) {
        for (ExecutionCommand cmd : s.getExecutionCommands(host)) {
          if (cmd.getTaskId() == taskId) {
            return s.getHostRoleCommand(host, cmd.getRole().toString());
          }
        }
      }
    }
    return null;
  }
}
