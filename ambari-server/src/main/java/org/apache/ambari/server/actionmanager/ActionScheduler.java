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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.agent.ActionQueue;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.controller.HostsMap;
import org.apache.ambari.server.serveraction.ServerAction;
import org.apache.ambari.server.serveraction.ServerActionManager;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpFailedEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.reflect.TypeToken;
import com.google.inject.persist.UnitOfWork;

/**
 * This class encapsulates the action scheduler thread.
 * Action schedule frequently looks at action database and determines if
 * there is an action that can be scheduled.
 */
class ActionScheduler implements Runnable {

  private static Logger LOG = LoggerFactory.getLogger(ActionScheduler.class);
  private final long actionTimeout;
  private final long sleepTime;
  private final UnitOfWork unitOfWork;
  private volatile boolean shouldRun = true;
  private Thread schedulerThread = null;
  private final ActionDBAccessor db;
  private final short maxAttempts;
  private final ActionQueue actionQueue;
  private final Clusters fsmObject;
  private boolean taskTimeoutAdjustment = true;
  private final HostsMap hostsMap;
  private final Object wakeupSyncObject = new Object();
  private final ServerActionManager serverActionManager;

  /**
   * true if scheduler should run ASAP.
   * We need this flag to avoid sleep in situations, when
   * we receive awake() request during running a scheduler iteration.
   */
  private boolean activeAwakeRequest = false;
  private Cache<Long, Map<String, List<String>>> clusterHostInfoCache;

  public ActionScheduler(long sleepTimeMilliSec, long actionTimeoutMilliSec,
      ActionDBAccessor db, ActionQueue actionQueue, Clusters fsmObject,
      int maxAttempts, HostsMap hostsMap, ServerActionManager serverActionManager, UnitOfWork unitOfWork) {
    this.sleepTime = sleepTimeMilliSec;
    this.hostsMap = hostsMap;
    this.actionTimeout = actionTimeoutMilliSec;
    this.db = db;
    this.actionQueue = actionQueue;
    this.fsmObject = fsmObject;
    this.maxAttempts = (short) maxAttempts;
    this.serverActionManager = serverActionManager;
    this.unitOfWork = unitOfWork;
    this.clusterHostInfoCache = CacheBuilder.newBuilder().
        expireAfterAccess(5, TimeUnit.MINUTES).
        build();
  }

  public void start() {
    schedulerThread = new Thread(this);
    schedulerThread.start();
  }

  public void stop() {
    shouldRun = false;
    schedulerThread.interrupt();
  }

  /**
   * Should be called from another thread when we want scheduler to
   * make a run ASAP (for example, to process desired configs of SCHs).
   * The method is guaranteed to return quickly.
   */
  public void awake() {
    synchronized (wakeupSyncObject) {
      activeAwakeRequest = true;
      wakeupSyncObject.notify();
    }
  }

  @Override
  public void run() {
    while (shouldRun) {
      try {
        synchronized (wakeupSyncObject) {
          if (!activeAwakeRequest) {
              wakeupSyncObject.wait(sleepTime);
          }
          activeAwakeRequest = false;
        }
        doWork();
      } catch (InterruptedException ex) {
        LOG.warn("Scheduler thread is interrupted going to stop", ex);
        shouldRun = false;
      } catch (Exception ex) {
        LOG.warn("Exception received", ex);
      } catch (Throwable t) {
        LOG.warn("ERROR", t);
      }
    }
  }

  public void doWork() throws AmbariException {
    try {
      unitOfWork.begin();

      List<Stage> stages = db.getStagesInProgress();
      if (LOG.isDebugEnabled()) {
        LOG.debug("Scheduler wakes up");
      }
      if (stages == null || stages.isEmpty()) {
        //Nothing to do
        if (LOG.isDebugEnabled()) {
          LOG.debug("No stage in progress..nothing to do");
        }
        return;
      }

      for (Stage s : stages) {
        List<ExecutionCommand> commandsToSchedule = new ArrayList<ExecutionCommand>();
        Map<String, RoleStats> roleStats = processInProgressStage(s, commandsToSchedule);
        //Check if stage is failed
        boolean failed = false;
        for (String role : roleStats.keySet()) {
          RoleStats stats = roleStats.get(role);
          if (LOG.isDebugEnabled()) {
            LOG.debug("Stats for role:" + role + ", stats=" + stats);
          }
          if (stats.isRoleFailed()) {
            failed = true;
            break;
          }
        }

        if(!failed) {
          // Prior stage may have failed and it may need to fail the whole request
          failed = hasPreviousStageFailed(s);
        }

        if (failed) {
          LOG.warn("Operation completely failed, aborting request id:"
              + s.getRequestId());
          db.abortOperation(s.getRequestId());
          return;
        }

        //Schedule what we have so far
        for (ExecutionCommand cmd : commandsToSchedule) {
          if (Role.valueOf(cmd.getRole()).equals(Role.AMBARI_SERVER_ACTION)) {
            try {
              long now = System.currentTimeMillis();
              String hostName = cmd.getHostname();
              String roleName = cmd.getRole().toString();

              s.setStartTime(hostName, roleName, now);
              s.setLastAttemptTime(hostName, roleName, now);
              s.incrementAttemptCount(hostName, roleName);
              s.setHostRoleStatus(hostName, roleName, HostRoleStatus.QUEUED);
              db.hostRoleScheduled(s, hostName, roleName);
              String actionName = cmd.getRoleParams().get(ServerAction.ACTION_NAME);
              this.serverActionManager.executeAction(actionName, cmd.getCommandParams());
              reportServerActionSuccess(s, cmd);
            } catch (AmbariException e) {
              LOG.warn("Could not execute server action " + cmd.toString(), e);
              reportServerActionFailure(s, cmd, e.getMessage());
            }
          } else {
            try {
              scheduleHostRole(s, cmd);
            } catch (InvalidStateTransitionException e) {
              LOG.warn("Could not schedule host role " + cmd.toString(), e);
              db.abortHostRole(cmd.getHostname(), s.getRequestId(), s.getStageId(), cmd.getRole());
            }
          }
        }

        //Check if ready to go to next stage
        boolean goToNextStage = true;
        for (String role : roleStats.keySet()) {
          RoleStats stats = roleStats.get(role);
          if (!stats.isSuccessFactorMet()) {
            goToNextStage = false;
            break;
          }
        }
        if (!goToNextStage) {
          return;
        }
      }

    } finally {
      unitOfWork.end();
    }
  }

  private boolean hasPreviousStageFailed(Stage stage) {
    boolean failed = false;
    long prevStageId = stage.getStageId() - 1;
    if (prevStageId > 0) {
      List<Stage> allStages = db.getAllStages(stage.getRequestId());
      Stage prevStage = null;
      for (Stage s : allStages) {
        if (s.getStageId() == prevStageId) {
          prevStage = s;
          break;
        }
      }

      //It may be null for test scenarios
      if(prevStage != null) {
        Map<Role, Integer> hostCountsForRoles = new HashMap<Role, Integer>();
        Map<Role, Integer> failedHostCountsForRoles = new HashMap<Role, Integer>();

        for (String host : prevStage.getHostRoleCommands().keySet()) {
          Map<String, HostRoleCommand> roleCommandMap = prevStage.getHostRoleCommands().get(host);
          for (String role : roleCommandMap.keySet()) {
            HostRoleCommand c = roleCommandMap.get(role);
            if (hostCountsForRoles.get(c.getRole()) == null) {
              hostCountsForRoles.put(c.getRole(), 0);
              failedHostCountsForRoles.put(c.getRole(), 0);
            }
            int hostCount = hostCountsForRoles.get(c.getRole());
            hostCountsForRoles.put(c.getRole(), hostCount + 1);
            if (c.getStatus().isFailedState()) {
              int failedHostCount = failedHostCountsForRoles.get(c.getRole());
              failedHostCountsForRoles.put(c.getRole(), failedHostCount + 1);
            }
          }
        }

        for (Role role : hostCountsForRoles.keySet()) {
          float failedHosts = failedHostCountsForRoles.get(role);
          float totalHosts = hostCountsForRoles.get(role);
          if (((totalHosts - failedHosts) / totalHosts) < prevStage.getSuccessFactor(role)) {
            failed = true;
          }
        }
      }
    }
    return failed;
  }

  private void reportServerActionSuccess(Stage stage, ExecutionCommand cmd) {
    CommandReport report = new CommandReport();
    report.setStatus(HostRoleStatus.COMPLETED.toString());
    report.setExitCode(0);
    report.setStdOut("Server action succeeded");
    report.setStdErr("");
    db.updateHostRoleState(cmd.getHostname(), stage.getRequestId(), stage.getStageId(),
                           cmd.getRole().toString(), report);
  }

  private void reportServerActionFailure(Stage stage, ExecutionCommand cmd, String message) {
    CommandReport report = new CommandReport();
    report.setStatus(HostRoleStatus.FAILED.toString());
    report.setExitCode(1);
    report.setStdOut("Server action failed");
    report.setStdErr(message);
    db.updateHostRoleState(cmd.getHostname(), stage.getRequestId(), stage.getStageId(),
                           cmd.getRole().toString(), report);
  }

  /**
   * @param commandsToSchedule
   * @return Stats for the roles in the stage. It is used to determine whether stage
   * has succeeded or failed.
   */
  private Map<String, RoleStats> processInProgressStage(Stage s,
      List<ExecutionCommand> commandsToSchedule) throws AmbariException {
    // Map to track role status
    Map<String, RoleStats> roleStats = initRoleStats(s);
    long now = System.currentTimeMillis();
    long taskTimeout = actionTimeout;
    if (taskTimeoutAdjustment) {
      taskTimeout = actionTimeout + s.getTaskTimeout();
    }
    for (String host : s.getHosts()) {
      List<ExecutionCommandWrapper> commandWrappers = s.getExecutionCommands(host);
      Cluster cluster = fsmObject.getCluster(s.getClusterName());
      Host hostObj = fsmObject.getHost(host);
      for(ExecutionCommandWrapper wrapper : commandWrappers) {
        ExecutionCommand c = wrapper.getExecutionCommand();
        String roleStr = c.getRole().toString();
        HostRoleStatus status = s.getHostRoleStatus(host, roleStr);
        if (timeOutActionNeeded(status, s, hostObj, roleStr, now,
          taskTimeout)) {
          LOG.info("Host:" + host + ", role:" + roleStr + ", actionId:"
              + s.getActionId() + " timed out");
          if (s.getAttemptCount(host, roleStr) >= maxAttempts) {
            LOG.warn("Host:" + host + ", role:" + roleStr + ", actionId:"
                + s.getActionId() + " expired");
            db.timeoutHostRole(host, s.getRequestId(), s.getStageId(), c.getRole());
            //Reinitialize status
            status = s.getHostRoleStatus(host, roleStr);
            ServiceComponentHostOpFailedEvent timeoutEvent =
                new ServiceComponentHostOpFailedEvent(roleStr,
                    host, now);
            try {
              Service svc = cluster.getService(c.getServiceName());
              ServiceComponent svcComp = svc.getServiceComponent(
                  roleStr);
              ServiceComponentHost svcCompHost =
                  svcComp.getServiceComponentHost(host);
              svcCompHost.handleEvent(timeoutEvent);
              LOG.warn("Operation timed out. Role: " + roleStr + ", host: " + host);
            } catch (ServiceComponentNotFoundException scnex) {
              LOG.debug("Not a service component, assuming its an action. Details: "
                  + scnex.getMessage());
            } catch (InvalidStateTransitionException e) {
              LOG.info("Transition failed for host: " + host + ", role: "
                  + roleStr, e);
            } catch (AmbariException ex) {
              LOG.warn("Invalid live state", ex);
            }
            // Dequeue command
            actionQueue.dequeue(host, c.getCommandId());
          } else {
            commandsToSchedule.add(c);
          }
        } else if (status.equals(HostRoleStatus.PENDING)) {
          //Need to schedule first time
          commandsToSchedule.add(c);
        }
        this.updateRoleStats(status, roleStats.get(roleStr));
      }
    }
    return roleStats;
  }

  private Map<String, RoleStats> initRoleStats(Stage s) {
    Map<Role, Integer> hostCountsForRoles = new HashMap<Role, Integer>();
    Map<String, RoleStats> roleStats = new TreeMap<String, RoleStats>();

    for (String host : s.getHostRoleCommands().keySet()) {
      Map<String, HostRoleCommand> roleCommandMap = s.getHostRoleCommands().get(host);
      for (String role : roleCommandMap.keySet()) {
        HostRoleCommand c = roleCommandMap.get(role);
        if (hostCountsForRoles.get(c.getRole()) == null) {
          hostCountsForRoles.put(c.getRole(), 0);
        }
        int val = hostCountsForRoles.get(c.getRole());
        hostCountsForRoles.put(c.getRole(), val + 1);
      }
    }

    for (Role r : hostCountsForRoles.keySet()) {
      RoleStats stats = new RoleStats(hostCountsForRoles.get(r),
          s.getSuccessFactor(r));
      roleStats.put(r.toString(), stats);
    }
    return roleStats;
  }

  private boolean timeOutActionNeeded(HostRoleStatus status, Stage stage,
      Host host, String role, long currentTime, long taskTimeout) throws
    AmbariException {
    if (( !status.equals(HostRoleStatus.QUEUED) ) &&
        ( ! status.equals(HostRoleStatus.IN_PROGRESS) )) {
      return false;
    }
    // Fast fail task if host state is unknown
    if (host.getState().equals(HostState.HEARTBEAT_LOST)) {
      LOG.debug("Timing out action since agent is not heartbeating.");
      return true;
    }
    if (currentTime > stage.getLastAttemptTime(host.getHostName(),
      role) + taskTimeout) {
      return true;
    }
    return false;
  }

  private void scheduleHostRole(Stage s, ExecutionCommand cmd)
      throws InvalidStateTransitionException, AmbariException {
    long now = System.currentTimeMillis();
    String roleStr = cmd.getRole().toString();
    String hostname = cmd.getHostname();
    if (s.getStartTime(hostname, roleStr) < 0) {
      try {
        Cluster c = fsmObject.getCluster(s.getClusterName());
        Service svc = c.getService(cmd.getServiceName());
        ServiceComponent svcComp = svc.getServiceComponent(roleStr);
        ServiceComponentHost svcCompHost =
            svcComp.getServiceComponentHost(hostname);
        svcCompHost.handleEvent(s.getFsmEvent(hostname, roleStr).getEvent());
      } catch (ServiceComponentNotFoundException scnex) {
        LOG.info("Not a service component, assuming its an action", scnex);
      } catch (InvalidStateTransitionException e) {
        LOG.info(
            "Transition failed for host: " + hostname + ", role: "
                + roleStr, e);
        throw e;
      } catch (AmbariException e) {
        LOG.warn("Exception in fsm: " + hostname + ", role: " + roleStr,
            e);
        throw e;
      }
      s.setStartTime(hostname,roleStr, now);
      s.setHostRoleStatus(hostname, roleStr, HostRoleStatus.QUEUED);
    }
    s.setLastAttemptTime(hostname, roleStr, now);
    s.incrementAttemptCount(hostname, roleStr);
    LOG.debug("Scheduling command: "+cmd.toString()+" for host: "+hostname);
    /** change the hostname in the command for the host itself **/
    cmd.setHostname(hostsMap.getHostMap(hostname));
    

    //Try to get clusterHostInfo from cache
    Map<String, List<String>> clusterHostInfo = clusterHostInfoCache.getIfPresent(s.getStageId());

    if (clusterHostInfo == null) {
      Type type = new TypeToken<Map<String, List<String>>>() {}.getType();
      clusterHostInfo = StageUtils.getGson().fromJson(s.getClusterHostInfo(), type);
      clusterHostInfoCache.put(s.getStageId(), clusterHostInfo);
    }
    
    cmd.setClusterHostInfo(clusterHostInfo);

    actionQueue.enqueue(hostname, cmd);
    db.hostRoleScheduled(s, hostname, roleStr);
  }

  private void updateRoleStats(HostRoleStatus status, RoleStats rs) {
    switch (status) {
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
      break;
    case IN_PROGRESS:
      rs.numInProgress++;
      break;
    default:
      LOG.error("Unknown status " + status.name());
    }
  }
  
  
  public void setTaskTimeoutAdjustment(boolean val) {
    this.taskTimeoutAdjustment = val;
  }

  static class RoleStats {
    int numInProgress;
    int numQueued = 0;
    int numSucceeded = 0;
    int numFailed = 0;
    int numTimedOut = 0;
    int numPending = 0;
    int numAborted = 0;
    final int totalHosts;
    final float successFactor;

    RoleStats(int total, float successFactor) {
      this.totalHosts = total;
      this.successFactor = successFactor;
    }

    /**
     * Role successful means the role is successful enough to
     */
    boolean isSuccessFactorMet() {
      int minSuccessNeeded = (int) Math.ceil(successFactor * totalHosts);
      if (minSuccessNeeded <= numSucceeded) {
        return true;
      } else {
        return false;
      }
    }

    private boolean isRoleInProgress() {
      return (numPending+numQueued+numInProgress > 0);
    }

    /**
     * Role failure means role is no longer in progress and success factor is
     * not met.
     */
    boolean isRoleFailed() {
      if (isRoleInProgress() || isSuccessFactorMet()) {
        return false;
      } else {
        return true;
      }
    }

    public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("numQueued="+numQueued);
      builder.append(", numInProgress="+numInProgress);
      builder.append(", numSucceeded="+numSucceeded);
      builder.append(", numFailed="+numFailed);
      builder.append(", numTimedOut="+numTimedOut);
      builder.append(", numPending="+numPending);
      builder.append(", numAborted="+numAborted);
      builder.append(", totalHosts="+totalHosts);
      builder.append(", successFactor="+successFactor);
      return builder.toString();
    }
  }
}
