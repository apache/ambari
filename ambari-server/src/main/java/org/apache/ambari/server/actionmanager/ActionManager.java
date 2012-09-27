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

import org.apache.ambari.server.agent.ActionQueue;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.state.live.Clusters;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * This class acts as the interface for action manager with other components.
 */
@Singleton
public class ActionManager {
  private final ActionScheduler scheduler;
  private final ActionDBAccessor db;
  private final ActionQueue actionQueue;
  private final Clusters fsm;

  @Inject
  public ActionManager(long schedulerSleepTime, long actionTimeout,
      ActionQueue aq, Clusters fsm) {
    this.actionQueue = aq;
    db = new ActionDBAccessorImpl();
    scheduler = new ActionScheduler(schedulerSleepTime, actionTimeout, db,
        actionQueue, fsm, 2);
    this.fsm = fsm;
  }
  
  public void initialize() {
    scheduler.start();
  }
  
  public void shutdown() {
    scheduler.stop();
  }
  
  public void sendActions(List<Stage> stages) {
    db.persistActions(stages);
  }

  public List<Stage> getRequestStatus(String requestId) {
    //fetch status from db
    return null;
  }

  public Stage getActionStatus(String actionId) {
    //fetch the action information from the db
    return null;
  }

  public void actionResponse(String hostname, List<CommandReport> report) {
    //persist the action response into the db.
  }

  public void handleLostHost(String host) {
    // TODO Auto-generated method stub
    
  }
}
