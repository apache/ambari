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
import org.apache.ambari.server.agent.CommandReport;

import com.google.inject.Singleton;

@Singleton
public class ActionDBAccessorImpl implements ActionDBAccessor {

  public ActionDBAccessorImpl() {
    //this.stageId = greatest stage id in the database + 1
  }

  /* (non-Javadoc)
   * @see org.apache.ambari.server.actionmanager.ActionDBAccessor#getAction(java.lang.String)
   */
  @Override
  public Stage getAction(String actionId) {
    return null;
  }

  /* (non-Javadoc)
   * @see org.apache.ambari.server.actionmanager.ActionDBAccessor#getAllStages(java.lang.String)
   */
  @Override
  public List<Stage> getAllStages(long requestId) {
    return null;
  }

  /* (non-Javadoc)
   * @see org.apache.ambari.server.actionmanager.ActionDBAccessor#abortOperation(long)
   */
  @Override
  public void abortOperation(long requestId) {
    //Mark all pending or queued actions for this request as aborted.
  }

  /* (non-Javadoc)
   * @see org.apache.ambari.server.actionmanager.ActionDBAccessor#timeoutHostRole(long, long, org.apache.ambari.server.Role)
   */
  @Override
  public void timeoutHostRole(String host, long requestId, long stageId, Role role) {
    // TODO Auto-generated method stub
  }

  /* (non-Javadoc)
   * @see org.apache.ambari.server.actionmanager.ActionDBAccessor#getPendingStages()
   */
  @Override
  public List<Stage> getStagesInProgress() {
    return null;
  }

  /* (non-Javadoc)
   * @see org.apache.ambari.server.actionmanager.ActionDBAccessor#persistActions(java.util.List)
   */
  @Override
  public void persistActions(List<Stage> stages) {
    // TODO Auto-generated method stub

  }

  @Override
  public void updateHostRoleState(String hostname, long requestId,
      long stageId, String role, CommandReport report) {
    // TODO Auto-generated method stub

  }

  @Override
  public void abortHostRole(String host, long requestId, long stageId, Role role) {
    // TODO Auto-generated method stub

  }

  @Override
  public long getLastPersistedRequestIdWhenInitialized() {
    // TODO Auto-generated method stub
    return 0;
  }
}
