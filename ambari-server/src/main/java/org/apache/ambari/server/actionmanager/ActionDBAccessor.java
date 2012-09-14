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

import java.util.List;

import org.apache.ambari.server.Role;

public class ActionDBAccessor {
  
  private long stageId = 0;
  
  public void persistAction(HostAction ha) {
  }

  public Stage getAction(String actionId) {
    return null;
  }

  public List<Stage> getAllStages(String requestId) {
    return null;
  }
  
  /**
   * Returns all the actions that have been queued but not completed yet.
   * This is used by scheduler to find all pending actions.
   */
  public List<Stage> getQueuedStages() {
    return null;
  }
  
  /**
   * Returns all the actions that have not been queued yet.
   */
  public List<Stage> getNotQueuedStages() {
    return null;
  }
  
  /**
   * Returns next stage id in the sequence, must be persisted.
   */
  public synchronized long getNextStageId() {
    return ++stageId ;
  }

  public void abortOperation(long requestId) {
    //Mark all pending or queued actions for this request as aborted.
  }

  public void timeoutHostRole(long requestId, long stageId, Role role) {
    // TODO Auto-generated method stub
  }
}
