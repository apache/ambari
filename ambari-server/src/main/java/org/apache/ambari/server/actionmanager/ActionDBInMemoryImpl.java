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
import java.util.List;

import org.apache.ambari.server.Role;

public class ActionDBInMemoryImpl implements ActionDBAccessor {

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
        for(String host: s.getHostActions().keySet()) {
          for (HostRoleCommand role : s.getHostActions().get(host).getRoleCommands()) {
            role.setStatus(HostRoleStatus.ABORTED);
          }
        }
      }
    }
  }

  @Override
  public synchronized void timeoutHostRole(String host, long requestId,
      long stageId, Role role) {
    for (Stage s : stageList) {
      for (HostRoleCommand r : s.getHostActions().get(host).getRoleCommands()) {
        if (r.getRole().equals(role)) {
          r.setStatus(HostRoleStatus.TIMEDOUT);
        }
      }
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
      stageList.add(s);
    }
  }
}
