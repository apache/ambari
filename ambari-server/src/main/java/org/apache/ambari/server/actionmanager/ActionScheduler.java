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
import java.util.Map;
import java.util.TreeMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.agent.ActionQueue;
import org.apache.ambari.server.state.fsm.InvalidStateTransitonException;
import org.apache.ambari.server.state.live.Clusters;
import org.apache.ambari.server.state.live.svccomphost.ServiceComponentHostEvent;
import org.apache.ambari.server.state.live.svccomphost.ServiceComponentHostEventType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//This class encapsulates the action scheduler thread.
//Action schedule frequently looks at action database and determines if
//there is an action that can be scheduled.
class ActionScheduler implements Runnable {

  private static Log LOG = LogFactory.getLog(ActionScheduler.class);
  private final long actionTimeout;
  private final long sleepTime;
  private volatile boolean shouldRun = true;
  private Thread schedulerThread = null;
  private final ActionDBAccessor db;
  private final short maxAttempts;
  private final ActionQueue actionQueue;
  private final Clusters fsmObject;

  public ActionScheduler(long sleepTimeMilliSec, long actionTimeoutMilliSec,
      ActionDBAccessor db, ActionQueue actionQueue, Clusters fsmObject,
      int maxAttempts) {
    this.sleepTime = sleepTimeMilliSec;
    this.actionTimeout = actionTimeoutMilliSec;
    this.db = db;
    this.actionQueue = actionQueue;
    this.fsmObject = fsmObject;
    this.maxAttempts = (short) maxAttempts;
  }

  public void start() {
    schedulerThread = new Thread(this);
    schedulerThread.start();
  }

  public void stop() {
    shouldRun = false;
    schedulerThread.interrupt();
  }

  @Override
  public void run() {
    while (shouldRun) {
      try {
        doWork();
        Thread.sleep(sleepTime);
      } catch (InterruptedException ex) {
        LOG.warn("Scheduler thread is interrupted going to stop", ex);
        shouldRun = false;
      } catch (Exception ex) {
        LOG.warn("Exception received", ex);
      }
    }
  }

  private void doWork() throws AmbariException {
    List<Stage> stages = db.getStagesInProgress();
    if (stages == null || stages.isEmpty()) {
      //Nothing to do
      return;
    }

    //First discover completions and timeouts.
    boolean operationFailure = false;
    for (Stage s : stages) {
      Map<Role, Map<String, HostRoleCommand>> roleToHrcMap = getInvertedRoleMap(s);

      //Iterate for completion
      boolean moveToNextStage = true;
      for (Role r: roleToHrcMap.keySet()) {
        processPendingsAndReschedule(s, roleToHrcMap.get(r));
        RoleStatus roleStatus = getRoleStatus(roleToHrcMap.get(r), s.getSuccessFactor(r));
        if (!roleStatus.isRoleSuccessful()) {
          if (!roleStatus.isRoleInProgress()) {
            //The role has completely failed
            //Mark the entire operation as failed
            operationFailure = true;
            break;
          }
          moveToNextStage = false;
        }
      }
      if (operationFailure) {
        db.abortOperation(s.getRequestId());
      }
      if (operationFailure || !moveToNextStage) {
        break;
      }
    }
  }

  private void processPendingsAndReschedule(Stage stage,
      Map<String, HostRoleCommand> hrcMap) throws AmbariException {
    for (String host : hrcMap.keySet()) {
      HostRoleCommand hrc = hrcMap.get(host);
      if ( (hrc.getStatus() != HostRoleStatus.PENDING) &&
           (hrc.getStatus() != HostRoleStatus.QUEUED) ) {
        //This task has been executed
        continue;
      }
      long now = System.currentTimeMillis();
      if (now > stage.getLastAttemptTime(host)+actionTimeout) {
        LOG.info("Host:"+host+", role:"+hrc.getRole()+", actionId:"+stage.getActionId()+" timed out");
        if (stage.getAttemptCount(host) >= maxAttempts) {
          LOG.warn("Host:"+host+", role:"+hrc.getRole()+", actionId:"+stage.getActionId()+" expired");
          // final expired
          ServiceComponentHostEvent timeoutEvent = new ServiceComponentHostEvent(
              ServiceComponentHostEventType.HOST_SVCCOMP_OP_FAILED, hrc
                  .getRole().toString(), host, now);
          try {
            fsmObject.getCluster(stage.getClusterName())
                .handleServiceComponentHostEvent("", hrc.getRole().toString(),
                    host, timeoutEvent);
          } catch (InvalidStateTransitonException e) {
            // Propagate exception
            e.printStackTrace();
          } catch (AmbariException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
          db.timeoutHostRole(host, stage.getRequestId(), stage.getStageId(),
              hrc.getRole());
        } else {
          scheduleHostRole(stage, host, hrc);
        }
      }
    }
  }

  private void scheduleHostRole(Stage s, String hostname, HostRoleCommand hrc) {
    LOG.info("Host:" + hostname + ", role:" + hrc.getRole() + ", actionId:"
        + s.getActionId() + " being scheduled");
    long now = System.currentTimeMillis();
    if (s.getStartTime(hostname) < 0) {
      try {
        fsmObject.getCluster(s.getClusterName())
            .handleServiceComponentHostEvent("", "", hostname,
                hrc.getEvent());
      } catch (InvalidStateTransitonException e) {
        e.printStackTrace();
      } catch (AmbariException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    s.setLastAttemptTime(hostname, now);
    s.incrementAttemptCount(hostname);
    actionQueue.enqueue(hostname, s.getExecutionCommand(hostname));
  }

  private RoleStatus getRoleStatus(
      Map<String, HostRoleCommand> hostRoleCmdForRole, float successFactor) {
    RoleStatus rs = new RoleStatus(hostRoleCmdForRole.size(), successFactor);
    for (String h : hostRoleCmdForRole.keySet()) {
      HostRoleCommand hrc = hostRoleCmdForRole.get(h);
      switch (hrc.getStatus()) {
      case COMPLETED:
        rs.numSucceeded++;
        break;
      case FAILED:
        rs.numFailed++;
        break;
      case QUEUED:
        rs.numQueued++;
        break;
      case PENDING:
        rs.numPending++;
        break;
      case TIMEDOUT:
        rs.numTimedOut++;
        break;
      case ABORTED:
        rs.numAborted++;
      }
    }
    return rs;
  }

  private Map<Role, Map<String, HostRoleCommand>> getInvertedRoleMap(Stage s) {
    // Temporary to store role to host
    Map<Role, Map<String, HostRoleCommand>> roleToHrcMap = new TreeMap<Role, Map<String, HostRoleCommand>>();
    Map<String, HostAction> hostActions = s.getHostActions();
    for (String h : hostActions.keySet()) {
      HostAction ha = hostActions.get(h);
      List<HostRoleCommand> roleCommands = ha.getRoleCommands();
      for (HostRoleCommand hrc : roleCommands) {
        Map<String, HostRoleCommand> hrcMap = roleToHrcMap.get(hrc.getRole());
        if (hrcMap == null) {
          hrcMap = new TreeMap<String, HostRoleCommand>();
          roleToHrcMap.put(hrc.getRole(), hrcMap);
        }
        hrcMap.put(h, hrc);
      }
    }
    return roleToHrcMap;
  }

  static class RoleStatus {
    int numQueued = 0;
    int numSucceeded = 0;
    int numFailed = 0;
    int numTimedOut = 0;
    int numPending = 0;
    int numAborted = 0;
    final int totalHosts;
    final float successFactor;

    RoleStatus(int total, float successFactor) {
      this.totalHosts = total;
      this.successFactor = successFactor;
    }

    boolean isRoleSuccessful() {
      if (successFactor <= (1.0*numSucceeded)/totalHosts) {
        return true;
      } else {
        return false;
      }
    }

    boolean isRoleInProgress() {
      return (numPending+numQueued > 0);
    }

    boolean isRoleFailed() {
      if ((!isRoleInProgress()) && (!isRoleSuccessful())) {
        return false;
      } else {
        return true;
      }
    }
  }
}
