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
import java.util.concurrent.atomic.AtomicLong;

import org.apache.ambari.server.agent.ActionQueue;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.utils.StageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;


/**
 * This class acts as the interface for action manager with other components.
 */
@Singleton
public class ActionManager {
  private final ActionScheduler scheduler;
  private final ActionDBAccessor db;
  private final ActionQueue actionQueue;
  private final Clusters fsm;
  private static Logger LOG = LoggerFactory.getLogger(ActionManager.class);
  private final AtomicLong requestCounter;

  @Inject
  public ActionManager(@Named("schedulerSleeptime") long schedulerSleepTime,
      @Named("actionTimeout") long actionTimeout,
      ActionQueue aq, Clusters fsm, ActionDBAccessor db) {
    this.actionQueue = aq;
    this.db = db;
    scheduler = new ActionScheduler(schedulerSleepTime, actionTimeout, db,
        actionQueue, fsm, 2);
    this.fsm = fsm;
    requestCounter = new AtomicLong(db.getLastPersistedRequestIdWhenInitialized());
  }

  public void start() {
    scheduler.start();
  }

  public void shutdown() {
    scheduler.stop();
  }

  public void sendActions(List<Stage> stages) {
    db.persistActions(stages);
  }

  public List<Stage> getRequestStatus(long requestId) {
    return db.getAllStages(requestId);
  }

  public Stage getAction(long requestId, long stageId) {
    return db.getAction(StageUtils.getActionId(requestId, stageId));
  }

  public void actionResponse(String hostname, List<CommandReport> reports) {
    if (reports == null) {
      return;
    }
    //persist the action response into the db.
    for (CommandReport report : reports) {
      String actionId = report.getActionId();
      long [] requestStageIds = StageUtils.getRequestStage(actionId);
      long requestId = requestStageIds[0];
      long stageId = requestStageIds[1];
      db.updateHostRoleState(hostname, requestId, stageId, report.getRole(),
          report);
    }
  }

  public void handleLostHost(String host) {
    //Do nothing, the task will timeout anyway.
    //The actions can be failed faster as an optimization
    //if action timeout happens to be much larger than
    //heartbeat timeout.
  }

  public long getNextRequestId() {
    return requestCounter.incrementAndGet();
  }

}
