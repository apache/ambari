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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.ServiceComponentHostNotFoundException;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.agent.ActionQueue;
import org.apache.ambari.server.agent.AgentCommand.AgentCommandType;
import org.apache.ambari.server.agent.CancelCommand;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.HostsMap;
import org.apache.ambari.server.events.ActionFinalReportReceivedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.serveraction.ServerActionExecutor;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostEvent;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpFailedEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.reflect.TypeToken;
import com.google.inject.persist.UnitOfWork;


/**
 * This class encapsulates the action scheduler thread.
 * Action schedule frequently looks at action database and determines if
 * there is an action that can be scheduled.
 */
class ActionScheduler implements Runnable {

  private static Logger LOG = LoggerFactory.getLogger(ActionScheduler.class);

  public static final String FAILED_TASK_ABORT_REASONING =
          "Server considered task failed and automatically aborted it";

  private final long actionTimeout;
  private final long sleepTime;
  private final UnitOfWork unitOfWork;
  private volatile boolean shouldRun = true;
  private Thread schedulerThread = null;
  private final ActionDBAccessor db;
  private final short maxAttempts;
  private final ActionQueue actionQueue;
  private final Clusters clusters;
  private final AmbariEventPublisher ambariEventPublisher;
  private boolean taskTimeoutAdjustment = true;
  private final HostsMap hostsMap;
  private final Object wakeupSyncObject = new Object();
  private final ServerActionExecutor serverActionExecutor;
  private final Configuration configuration;

  private final Set<Long> requestsInProgress = new HashSet<Long>();

  /**
   * Contains request ids that have been scheduled to be cancelled,
   * but are not cancelled yet
   */
  private final Set<Long> requestsToBeCancelled =
          Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());

  /**
   * Maps request IDs to reasoning for cancelling request.
   * Map is NOT synchronized, so any access to it should synchronize on
   * requestsToBeCancelled object
   */
  private final Map<Long, String> requestCancelReasons =
          new HashMap<Long, String>();

  /**
   * true if scheduler should run ASAP.
   * We need this flag to avoid sleep in situations, when
   * we receive awake() request during running a scheduler iteration.
   */
  private boolean activeAwakeRequest = false;
  //Cache for clusterHostinfo, key - stageId-requestId
  private Cache<String, Map<String, Set<String>>> clusterHostInfoCache;
  private Cache<String, Map<String, String>> commandParamsStageCache;
  private Cache<String, Map<String, String>> hostParamsStageCache;

  public ActionScheduler(long sleepTimeMilliSec, long actionTimeoutMilliSec,
                         ActionDBAccessor db, ActionQueue actionQueue, Clusters clusters,
                         int maxAttempts, HostsMap hostsMap,
                         UnitOfWork unitOfWork, AmbariEventPublisher ambariEventPublisher,
                         Configuration configuration) {
    sleepTime = sleepTimeMilliSec;
    this.hostsMap = hostsMap;
    actionTimeout = actionTimeoutMilliSec;
    this.db = db;
    this.actionQueue = actionQueue;
    this.clusters = clusters;
    this.ambariEventPublisher = ambariEventPublisher;
    this.maxAttempts = (short) maxAttempts;
    serverActionExecutor = new ServerActionExecutor(db, sleepTimeMilliSec);
    this.unitOfWork = unitOfWork;
    clusterHostInfoCache = CacheBuilder.newBuilder().
        expireAfterAccess(5, TimeUnit.MINUTES).
        build();
    commandParamsStageCache = CacheBuilder.newBuilder().
      expireAfterAccess(5, TimeUnit.MINUTES).
      build();
    hostParamsStageCache = CacheBuilder.newBuilder().
      expireAfterAccess(5, TimeUnit.MINUTES).
      build();
    this.configuration = configuration;
  }

  public void start() {
    schedulerThread = new Thread(this, "ambari-action-scheduler");
    schedulerThread.start();

    // Start up the ServerActionExecutor. Since it is directly related to the ActionScheduler it
    // should be started and stopped along with it.
    serverActionExecutor.start();
  }

  public void stop() {
    shouldRun = false;
    schedulerThread.interrupt();

    // Stop the ServerActionExecutor. Since it is directly related to the ActionScheduler it should
    // be started and stopped along with it.
    serverActionExecutor.stop();
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
        requestsInProgress.clear();
      } catch (Throwable t) {
        LOG.warn("ERROR", t);
        requestsInProgress.clear();
      }
    }
  }

  public void doWork() throws AmbariException {
    try {
      unitOfWork.begin();

      // The first thing to do is to abort requests that are cancelled
      processCancelledRequestsList();

      // !!! getting the stages in progress could be a very expensive call due
      // to the join being used; there's no need to make it if there are
      // no commands in progress
      if (db.getCommandsInProgressCount() == 0) {
        // Nothing to do
        if (LOG.isDebugEnabled()) {
          LOG.debug("There are no stages currently in progress.");
        }

        actionQueue.updateListOfHostsWithPendingTask(null);
        return;
      }

      Set<Long> runningRequestIds = new HashSet<Long>();
      List<Stage> stages = db.getStagesInProgress();
      if (LOG.isDebugEnabled()) {
        LOG.debug("Scheduler wakes up");
        LOG.debug("Processing {} in progress stages ", stages.size());
      }

      if (stages.isEmpty()) {
        // Nothing to do
        if (LOG.isDebugEnabled()) {
          LOG.debug("There are no stages currently in progress.");
        }

        actionQueue.updateListOfHostsWithPendingTask(null);
        return;
      }

      int i_stage = 0;

      HashSet<String> hostsWithTasks = getListOfHostsWithPendingTask(stages);
      actionQueue.updateListOfHostsWithPendingTask(hostsWithTasks);

      stages = filterParallelPerHostStages(stages);
      // At this point the stages is a filtered list

      boolean exclusiveRequestIsGoing = false;
      // This loop greatly depends on the fact that order of stages in
      // a list does not change between invocations
      for (Stage stage : stages) {
        // Check if we can process this stage in parallel with another stages
        i_stage ++;
        long requestId = stage.getRequestId();
        LOG.debug("==> STAGE_i = " + i_stage + "(requestId=" + requestId + ",StageId=" + stage.getStageId() + ")");

        RequestEntity request = db.getRequestEntity(requestId);

        if (request.isExclusive()) {
          if (runningRequestIds.size() > 0 ) {
            // As a result, we will wait until any previous stages are finished
            LOG.debug("Stage requires exclusive execution, but other requests are already executing. Stopping for now");
            break;
          }
          exclusiveRequestIsGoing = true;
        }

        if (runningRequestIds.contains(requestId)) {
          // We don't want to process different stages from the same request in parallel
          LOG.debug("==> We don't want to process different stages from the same request in parallel" );
          continue;
        } else {
          runningRequestIds.add(requestId);
          if (!requestsInProgress.contains(requestId)) {
            requestsInProgress.add(requestId);
            db.startRequest(requestId);
          }
        }

        // Commands that will be scheduled in current scheduler wakeup
        List<ExecutionCommand> commandsToSchedule = new ArrayList<ExecutionCommand>();
        Map<String, RoleStats> roleStats = processInProgressStage(stage, commandsToSchedule);

        // Check if stage is failed
        boolean failed = false;
        for (Map.Entry<String, RoleStats> entry : roleStats.entrySet()) {

          String role = entry.getKey();
          RoleStats stats = entry.getValue();

          if (LOG.isDebugEnabled()) {
            LOG.debug("Stats for role: {}, stats={}", role, stats);
          }

          // only fail the request if the role failed and the stage is not
          // skippable
          if (stats.isRoleFailed() && !stage.isSkippable()) {
            LOG.warn("{} failed, request {} will be aborted", role, request.getRequestId());

            failed = true;
            break;
          }
        }

        if (!failed) {
          // Prior stage may have failed and it may need to fail the whole request
          failed = hasPreviousStageFailed(stage);
        }

        if (failed) {
          LOG.warn("Operation completely failed, aborting request id: {}", stage.getRequestId());
          cancelHostRoleCommands(stage.getOrderedHostRoleCommands(), FAILED_TASK_ABORT_REASONING);
          abortOperationsForStage(stage);
          return;
        }

        List<ExecutionCommand> commandsToStart = new ArrayList<ExecutionCommand>();
        List<ExecutionCommand> commandsToUpdate = new ArrayList<ExecutionCommand>();

        //Schedule what we have so far

        for (ExecutionCommand cmd : commandsToSchedule) {

          // Hack - Remove passwords from configs
          if ((cmd.getRole().equals(Role.HIVE_CLIENT.toString()) ||
                  cmd.getRole().equals(Role.WEBHCAT_SERVER.toString()) ||
                  cmd.getRole().equals(Role.HCAT.toString())) &&
                  cmd.getConfigurations().containsKey(Configuration.HIVE_CONFIG_TAG)) {
            cmd.getConfigurations().get(Configuration.HIVE_CONFIG_TAG).remove(Configuration.HIVE_METASTORE_PASSWORD_PROPERTY);
          }
          processHostRole(stage, cmd, commandsToStart, commandsToUpdate);
        }

        LOG.debug("==> Commands to start: {}", commandsToStart.size());
        LOG.debug("==> Commands to update: {}", commandsToUpdate.size());

        //Multimap is analog of Map<Object, List<Object>> but allows to avoid nested loop
        ListMultimap<String, ServiceComponentHostEvent> eventMap = formEventMap(stage, commandsToStart);
        Map<ExecutionCommand, String> commandsToAbort = new HashMap<ExecutionCommand, String>();
        if (!eventMap.isEmpty()) {
          LOG.debug("==> processing {} serviceComponentHostEvents...", eventMap.size());
          Cluster cluster = clusters.getCluster(stage.getClusterName());
          if (cluster != null) {
            Map<ServiceComponentHostEvent, String> failedEvents = cluster.processServiceComponentHostEvents(eventMap);

            if (failedEvents.size() > 0) {
              LOG.error("==> {} events failed.", failedEvents.size());
            }

            for (Iterator<ExecutionCommand> iterator = commandsToUpdate.iterator(); iterator.hasNext(); ) {
              ExecutionCommand cmd = iterator.next();
              for (ServiceComponentHostEvent event : failedEvents.keySet()) {
                if (StringUtils.equals(event.getHostName(), cmd.getHostname()) &&
                  StringUtils.equals(event.getServiceComponentName(), cmd.getRole())) {
                  iterator.remove();
                  commandsToAbort.put(cmd, failedEvents.get(event));
                  break;
                }
              }
            }
          } else {
            LOG.warn("There was events to process but cluster {} not found", stage.getClusterName());
          }
        }

        LOG.debug("==> Scheduling {} tasks...", commandsToUpdate.size());
        db.bulkHostRoleScheduled(stage, commandsToUpdate);

        if (commandsToAbort.size() > 0) { // Code branch may be a bit slow, but is extremely rarely used
          LOG.debug("==> Aborting {} tasks...", commandsToAbort.size());
          // Build a list of HostRoleCommands
          List<Long> taskIds = new ArrayList<Long>();
          for (ExecutionCommand command : commandsToAbort.keySet()) {
            taskIds.add(command.getTaskId());
          }
          Collection<HostRoleCommand> hostRoleCommands = db.getTasks(taskIds);

          cancelHostRoleCommands(hostRoleCommands, FAILED_TASK_ABORT_REASONING);
          db.bulkAbortHostRole(stage, commandsToAbort);
        }

        LOG.debug("==> Adding {} tasks to queue...", commandsToUpdate.size());
        for (ExecutionCommand cmd : commandsToUpdate) {
          // Do not queue up server actions; however if we encounter one, wake up the ServerActionExecutor
          if (Role.AMBARI_SERVER_ACTION.name().equals(cmd.getRole())) {
            serverActionExecutor.awake();
          } else {
            actionQueue.enqueue(cmd.getHostname(), cmd);
          }
        }
        LOG.debug("==> Finished.");

        if (! configuration.getParallelStageExecution()) { // If disabled
          return;
        }

        if (exclusiveRequestIsGoing) {
          // As a result, we will prevent any further stages from being executed
          LOG.debug("Stage requires exclusive execution, skipping all executing any further stages");
          break;
        }
      }

      requestsInProgress.retainAll(runningRequestIds);

    } finally {
      LOG.debug("Scheduler finished work.");
      unitOfWork.end();
    }
  }

  /**
   * Returns the list of hosts that have a task assigned
   *
   * @param stages
   *
   * @return
   */
  private HashSet<String> getListOfHostsWithPendingTask(List<Stage> stages) {
    HashSet<String> hostsWithTasks = new HashSet<String>();
    for (Stage s : stages) {
      hostsWithTasks.addAll(s.getHosts());
    }
    return hostsWithTasks;
  }

  /**
   * Returns filtered list of stages such that the returned list is an ordered list of stages that may
   * be executed in parallel or in the order in which they are presented
   * <p/>
   * Assumption: the list of stages supplied as input are ordered by request id and then stage id.
   * <p/>
   * Rules:
   * <ul>
   * <li>
   * Stages are filtered such that the first stage in the list (assumed to be the first pending
   * stage from the earliest active request) has priority
   * </li>
   * <li>
   * No stage in any request may be executed before an earlier stage in the same request
   * </li>
   * <li>
   * A stages in different requests may be performed in parallel if the relevant hosts for the
   * stage in the later requests do not intersect with the union of hosts from (pending) stages
   * in earlier requests
   * </li>
   * </ul>
   *
   * @param stages the stages to process
   * @return a list of stages that may be executed in parallel
   */
  private List<Stage> filterParallelPerHostStages(List<Stage> stages) {
    List<Stage> retVal = new ArrayList<Stage>();
    Set<String> affectedHosts = new HashSet<String>();
    Set<Long> affectedRequests = new HashSet<Long>();

    for (Stage s : stages) {
      long requestId = s.getRequestId();

      if (LOG.isTraceEnabled()) {
        LOG.trace("==> Processing stage: {}/{} ({}) for {}", requestId, s.getStageId(), s.getRequestContext());
      }

      boolean addStage = true;

      // Iterate over the relevant hosts for this stage to see if any intersect with the set of
      // hosts needed for previous stages.  If any intersection occurs, this stage may not be
      // executed in parallel.
      for (String host : s.getHosts()) {
        LOG.trace("===> Processing Host {}", host);

        if (affectedHosts.contains(host)) {
          if (LOG.isTraceEnabled()) {
            LOG.trace("===>  Skipping stage since it utilizes at least one host that a previous stage requires: {}/{} ({})", s.getRequestId(), s.getStageId(), s.getRequestContext());
          }

          addStage &= false;
        } else {
          if (!Stage.INTERNAL_HOSTNAME.equalsIgnoreCase(host) && !isStageHasBackgroundCommandsOnly(s, host)) {
            LOG.trace("====>  Adding host to affected hosts: {}", host);
            affectedHosts.add(host);
          }

          addStage &= true;
        }
      }

      // If this stage is for a request that we have already processed, the it cannot execute in
      // parallel since only one stage per request my execute at a time. The first time we encounter
      // a request id, will be for the first pending stage for that request, so it is a candidate
      // for execution at this time - if the previous test for host intersection succeeds.
      if (affectedRequests.contains(requestId)) {
        if (LOG.isTraceEnabled()) {
          LOG.trace("===>  Skipping stage since the request it is in has been processed already: {}/{} ({})", s.getRequestId(), s.getStageId(), s.getRequestContext());
        }

        addStage = false;
      } else {
        if (LOG.isTraceEnabled()) {
          LOG.trace("====>  Adding request to affected requests: {}", requestId);
        }

        affectedRequests.add(requestId);
        addStage &= true;
      }

      // If both tests pass - the stage is the first pending stage in its request and the hosts
      // required in the stage do not intersect with hosts from stages that should occur before this,
      // than add it to the list of stages that may be executed in parallel.
      if (addStage) {
        if (LOG.isTraceEnabled()) {
          LOG.trace("===>  Adding stage to return value: {}/{} ({})", s.getRequestId(), s.getStageId(), s.getRequestContext());
        }

        retVal.add(s);
      }
    }

    return retVal;
  }

  private boolean isStageHasBackgroundCommandsOnly(Stage s, String host) {
    for (ExecutionCommandWrapper c : s.getExecutionCommands(host)) {
      if(c.getExecutionCommand().getCommandType() != AgentCommandType.BACKGROUND_EXECUTION_COMMAND)
      {
        return false;
      }
    }
    return true;
  }

  private boolean hasPreviousStageFailed(Stage stage) {
    boolean failed = false;

    long prevStageId = stage.getStageId() - 1;

    if (prevStageId > 0) {
      // Find previous stage instance
      String actionId = StageUtils.getActionId(stage.getRequestId(), prevStageId);
      Stage prevStage = db.getStage(actionId);

      // If the previous stage is skippable then we shouldn't automatically fail the given stage
      if (prevStage == null || prevStage.isSkippable()) {
        return false;
      }

      Map<Role, Integer> hostCountsForRoles       = new HashMap<Role, Integer>();
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
    return failed;
  }

  /**
   * This method processes command timeouts and retry attempts, and
   * adds new (pending) execution commands to commandsToSchedule list.
   *
   * @return the stats for the roles in the stage which are used to determine
   * whether stage has succeeded or failed
   */
  private Map<String, RoleStats> processInProgressStage(Stage s,
      List<ExecutionCommand> commandsToSchedule) throws AmbariException {
    LOG.debug("==> Collecting commands to schedule...");
    // Map to track role status
    Map<String, RoleStats> roleStats = initRoleStats(s);
    long now = System.currentTimeMillis();

    Cluster cluster = null;
    if (null != s.getClusterName()) {
      cluster = clusters.getCluster(s.getClusterName());
    }

    for (String host : s.getHosts()) {

      List<ExecutionCommandWrapper> commandWrappers = s.getExecutionCommands(host);
      Host hostObj = null;
      try {
        hostObj = clusters.getHost(host);
      } catch (AmbariException e) {
        LOG.debug("Host {} not found, stage is likely a server side action", host);
      }

      int i_my = 0;
      LOG.trace("===>host=" + host);

      for(ExecutionCommandWrapper wrapper : commandWrappers) {
        ExecutionCommand c = wrapper.getExecutionCommand();
        String roleStr = c.getRole();
        HostRoleStatus status = s.getHostRoleStatus(host, roleStr);
        i_my ++;
        if (LOG.isTraceEnabled()) {
          LOG.trace("Host task " + i_my + ") id = " + c.getTaskId() + " status = " + status.toString() +
            " (role=" + roleStr + "), roleCommand = "+ c.getRoleCommand());
        }
        boolean hostDeleted = false;
        if (null != cluster) {
          Service svc = null;
          if (c.getServiceName() != null && !c.getServiceName().isEmpty()) {
            svc = cluster.getService(c.getServiceName());
          }

          ServiceComponent svcComp = null;
          Map<String, ServiceComponentHost> scHosts = null;
          try {
            if (svc != null) {
              svcComp = svc.getServiceComponent(roleStr);
              scHosts = svcComp.getServiceComponentHosts();
            }
          } catch (ServiceComponentNotFoundException scnex) {
            String msg = String.format(
                    "%s is not not a service component, assuming its an action",
                    roleStr);
            LOG.debug(msg);
          }

          hostDeleted = (scHosts != null && !scHosts.containsKey(host));
          if (hostDeleted) {
            String message = String.format(
              "Host component information has not been found.  Details:" +
              "cluster=%s; host=%s; service=%s; component=%s; ",
              c.getClusterName(), host,
              svcComp == null ? "null" : svcComp.getServiceName(),
              svcComp == null ? "null" : svcComp.getName());
            LOG.warn(message);
          }
        }

        //basic timeout for stage
        long commandTimeout = actionTimeout;
        if (taskTimeoutAdjustment) {
          Map<String, String> commandParams = c.getCommandParams();
          String timeoutKey = ExecutionCommand.KeyNames.COMMAND_TIMEOUT;
          if (commandParams != null && commandParams.containsKey(timeoutKey)) {
            String timeoutStr = commandParams.get(timeoutKey);
            commandTimeout += Long.parseLong(timeoutStr) * 1000; // Converting to milliseconds
          } else {
            LOG.error("Execution command has no timeout parameter" +
                    c.toString());
          }
        }

        // Check that service host component is not deleted
        if (hostDeleted) {

          String message = String.format(
            "Host not found when trying to schedule an execution command. " +
            "The most probable reason for that is that host or host component " +
            "has been deleted recently. The command has been aborted and dequeued." +
            "Execution command details: " +
            "cmdId: %s; taskId: %s; roleCommand: %s",
            c.getCommandId(), c.getTaskId(), c.getRoleCommand());
          LOG.warn("Host {} has been detected as non-available. {}", host, message);
          // Abort the command itself
          // We don't need to send CANCEL_COMMANDs in this case
          db.abortHostRole(host, s.getRequestId(), s.getStageId(), c.getRole(), message);
          if (c.getRoleCommand().equals(RoleCommand.ACTIONEXECUTE)) {
            processActionDeath(cluster.getClusterName(), c.getHostname(), roleStr);
          }
          status = HostRoleStatus.ABORTED;
        } else if (timeOutActionNeeded(status, s, hostObj, roleStr, now, commandTimeout)) {
          // Process command timeouts
          LOG.info("Host:" + host + ", role:" + roleStr + ", actionId:" + s.getActionId() + " timed out");
          if (s.getAttemptCount(host, roleStr) >= maxAttempts) {
            LOG.warn("Host:" + host + ", role:" + roleStr + ", actionId:" + s.getActionId() + " expired");
            db.timeoutHostRole(host, s.getRequestId(), s.getStageId(), c.getRole());
            //Reinitialize status
            status = s.getHostRoleStatus(host, roleStr);

            if (null != cluster) {
              transitionToFailedState(cluster.getClusterName(), c.getServiceName(), roleStr, host, now, false);
              if (c.getRoleCommand().equals(RoleCommand.ACTIONEXECUTE)) {
                processActionDeath(cluster.getClusterName(), c.getHostname(), roleStr);
              }
            }

            // Dequeue command
            LOG.info("Removing command from queue, host={}, commandId={} ", host, c.getCommandId());
            actionQueue.dequeue(host, c.getCommandId());
          } else {
            // reschedule command
            commandsToSchedule.add(c);
            LOG.trace("===> commandsToSchedule(reschedule)=" + commandsToSchedule.size());
          }
        } else if (status.equals(HostRoleStatus.PENDING)) {
          //Need to schedule first time
          commandsToSchedule.add(c);
          LOG.trace("===>commandsToSchedule(first_time)=" + commandsToSchedule.size());
        }

        updateRoleStats(status, roleStats.get(roleStr));
      }
    }
    LOG.debug("Collected {} commands to schedule in this wakeup.", commandsToSchedule.size());
    return roleStats;
  }

  /**
   * Generate a OPFailed event before aborting all operations in the stage
   * @param stage
   */
  private void abortOperationsForStage(Stage stage) {
    long now = System.currentTimeMillis();

    for (String hostName : stage.getHosts()) {
      List<ExecutionCommandWrapper> commandWrappers =
        stage.getExecutionCommands(hostName);

      for(ExecutionCommandWrapper wrapper : commandWrappers) {
        ExecutionCommand c = wrapper.getExecutionCommand();
        transitionToFailedState(stage.getClusterName(), c.getServiceName(),
                c.getRole(), hostName, now, true);
        if (c.getRoleCommand().equals(RoleCommand.ACTIONEXECUTE)) {
          String clusterName = c.getClusterName();
          processActionDeath(clusterName,
                  c.getHostname(),
                  c.getRole());
        }
      }
    }
    db.abortOperation(stage.getRequestId());
  }

  /**
   * Raise a OPFailed event for a SCH
   * @param clusterName
   * @param serviceName
   * @param componentName
   * @param hostname
   * @param timestamp
   */
  private void transitionToFailedState(String clusterName, String serviceName,
                                       String componentName, String hostname,
                                       long timestamp,
                                       boolean ignoreTransitionException) {

    try {
      Cluster cluster = clusters.getCluster(clusterName);

      ServiceComponentHostOpFailedEvent failedEvent =
        new ServiceComponentHostOpFailedEvent(componentName,
          hostname, timestamp);

      if (serviceName != null && ! serviceName.isEmpty() &&
              componentName != null && ! componentName.isEmpty()) {
        Service svc = cluster.getService(serviceName);
        ServiceComponent svcComp = svc.getServiceComponent(componentName);
        ServiceComponentHost svcCompHost =
                svcComp.getServiceComponentHost(hostname);
        svcCompHost.handleEvent(failedEvent);
      } else {
        LOG.info("Service name is " + serviceName + ", component name is " + componentName +
                "skipping sending ServiceComponentHostOpFailedEvent for " + componentName);
      }

    } catch (ServiceComponentNotFoundException scnex) {
      LOG.debug(componentName + " associated with service " + serviceName +
        " is not a service component, assuming it's an action.");
    } catch (ServiceComponentHostNotFoundException e) {
      String msg = String.format("Service component host %s not found, " +
              "unable to transition to failed state.", componentName);
      LOG.warn(msg, e);
    } catch (InvalidStateTransitionException e) {
      if (ignoreTransitionException) {
        LOG.debug("Unable to transition to failed state.", e);
      } else {
        LOG.warn("Unable to transition to failed state.", e);
      }
    } catch (AmbariException e) {
      LOG.warn("Unable to transition to failed state.", e);
    }
  }


  /**
   * Populates a map < role_name, role_stats>.
   */
  private Map<String, RoleStats> initRoleStats(Stage s) {
    // Meaning: how many hosts are affected by commands for each role
    Map<Role, Integer> hostCountsForRoles = new HashMap<Role, Integer>();
    // < role_name, rolestats >
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
      roleStats.put(r.name(), stats);
    }
    return roleStats;
  }

  /**
   * Checks if timeout is required.
   * @param status      the status of the current role
   * @param stage       the stage
   * @param host        the host object; can be {@code null} for server-side tasks
   * @param role        the role
   * @param currentTime the current
   * @param taskTimeout the amount of time to determine timeout
   * @return {@code true} if timeout is needed
   * @throws AmbariException
   */
  private boolean timeOutActionNeeded(HostRoleStatus status, Stage stage,
      Host host, String role, long currentTime, long taskTimeout) throws
    AmbariException {
    if (( !status.equals(HostRoleStatus.QUEUED) ) &&
        ( ! status.equals(HostRoleStatus.IN_PROGRESS) )) {
      return false;
    }

    // Fast fail task if host state is unknown
    if (null != host && host.getState().equals(HostState.HEARTBEAT_LOST)) {
      LOG.debug("Timing out action since agent is not heartbeating.");
      return true;
    }

    // tasks are held in a variety of in-memory maps that require a hostname key
    // host being null is ok - that means it's a server-side task
    String hostName = (null == host) ? null : host.getHostName();

    // If we have other command in progress for this stage do not timeout this one
    if (hasCommandInProgress(stage, hostName)
            && !status.equals(HostRoleStatus.IN_PROGRESS)) {
      return false;
    }
    if (currentTime >= stage.getLastAttemptTime(hostName, role)
        + taskTimeout) {
      return true;
    }
    return false;
  }

  /**
   * Determines if at least one task for a given hostname in the specified stage is in progress.
   * <p/>
   * If the specified hostname is <code>null</code>, the Ambari Server host is assumed.
   * See {@link Stage#getSafeHost(String)}.
   *
   * @param stage    a stage
   * @param hostname a host name, if null the Ambari Server host is assumed
   * @return true if at least one task for the given hostname in the specified stage is in progress; otherwize false
   * @see Stage#getExecutionCommands(String)
   * @see Stage#getHostRoleStatus(String, String)
   */
  private boolean hasCommandInProgress(Stage stage, String hostname) {
    List<ExecutionCommandWrapper> commandWrappers = stage.getExecutionCommands(hostname);
    for (ExecutionCommandWrapper wrapper : commandWrappers) {
      ExecutionCommand c = wrapper.getExecutionCommand();
      String roleStr = c.getRole();
      HostRoleStatus status = stage.getHostRoleStatus(hostname, roleStr);
      if (status == HostRoleStatus.IN_PROGRESS) {
        return true;
      }
    }
    return false;
  }

  private ListMultimap<String, ServiceComponentHostEvent> formEventMap(Stage s, List<ExecutionCommand> commands) {
    ListMultimap<String, ServiceComponentHostEvent> serviceEventMap = ArrayListMultimap.create();
    for (ExecutionCommand cmd : commands) {
      String hostname = cmd.getHostname();
      String roleStr = cmd.getRole();
      if (RoleCommand.ACTIONEXECUTE != cmd.getRoleCommand()) {
          serviceEventMap.put(cmd.getServiceName(), s.getFsmEvent(hostname, roleStr).getEvent());
      }
    }
    return serviceEventMap;
  }

  private void processHostRole(Stage s, ExecutionCommand cmd, List<ExecutionCommand> commandsToStart,
                               List<ExecutionCommand> commandsToUpdate)
    throws AmbariException {
    long now = System.currentTimeMillis();
    String roleStr = cmd.getRole();
    String hostname = cmd.getHostname();

    // start time is -1 if host role command is not started yet
    if (s.getStartTime(hostname, roleStr) < 0) {

      commandsToStart.add(cmd);
      s.setStartTime(hostname,roleStr, now);
      s.setHostRoleStatus(hostname, roleStr, HostRoleStatus.QUEUED);
    }
    s.setLastAttemptTime(hostname, roleStr, now);
    s.incrementAttemptCount(hostname, roleStr);
    /** change the hostname in the command for the host itself **/
    cmd.setHostname(hostsMap.getHostMap(hostname));


    //Try to get clusterHostInfo from cache
    String stagePk = s.getStageId() + "-" + s.getRequestId();
    Map<String, Set<String>> clusterHostInfo = clusterHostInfoCache.getIfPresent(stagePk);

    if (clusterHostInfo == null) {
      Type type = new TypeToken<Map<String, Set<String>>>() {}.getType();
      clusterHostInfo = StageUtils.getGson().fromJson(s.getClusterHostInfo(), type);
      clusterHostInfoCache.put(stagePk, clusterHostInfo);
    }

    cmd.setClusterHostInfo(clusterHostInfo);

    //Try to get commandParams from cache and merge them with command-level parameters
    Map<String, String> commandParams = commandParamsStageCache.getIfPresent(stagePk);

    if (commandParams == null){
      Type type = new TypeToken<Map<String, String>>() {}.getType();
      commandParams = StageUtils.getGson().fromJson(s.getCommandParamsStage(), type);
      commandParamsStageCache.put(stagePk, commandParams);
    }
    Map<String, String> commandParamsCmd = cmd.getCommandParams();
    commandParamsCmd.putAll(commandParams);
    cmd.setCommandParams(commandParamsCmd);

    try {
      Cluster cluster = clusters.getCluster(s.getClusterName());
      if (null != cluster) {
        // Generate localComponents
        for (ServiceComponentHost sch : cluster.getServiceComponentHosts(hostname)) {
          cmd.getLocalComponents().add(sch.getServiceComponentName());
        }
      }
    } catch (ClusterNotFoundException cnfe) {
      //NOP
    }

    //Try to get hostParams from cache and merge them with command-level parameters
    Map<String, String> hostParams = hostParamsStageCache.getIfPresent(stagePk);
    if (hostParams == null) {
      Type type = new TypeToken<Map<String, String>>() {}.getType();
      hostParams = StageUtils.getGson().fromJson(s.getHostParamsStage(), type);
      hostParamsStageCache.put(stagePk, hostParams);
    }
    Map<String, String> hostParamsCmd = cmd.getHostLevelParams();
    hostParamsCmd.putAll(hostParams);
    cmd.setHostLevelParams(hostParamsCmd);


    commandsToUpdate.add(cmd);
  }

  /**
   * @param requestId request will be cancelled on next scheduler wake up
   * (if it is in state that allows cancellation, e.g. QUEUED, PENDING, IN_PROGRESS)
   * @param reason why request is being cancelled
   */
  public void scheduleCancellingRequest(long requestId, String reason) {
    synchronized (requestsToBeCancelled) {
      requestsToBeCancelled.add(requestId);
      requestCancelReasons.put(requestId, reason);
    }
  }


  /**
   * Aborts all stages that belong to requests that are being cancelled
   */
  private void processCancelledRequestsList() {
    synchronized (requestsToBeCancelled) {
      // Now, cancel stages completely
      for (Long requestId : requestsToBeCancelled) {
        List<HostRoleCommand> tasksToDequeue = db.getRequestTasks(requestId);
        String reason = requestCancelReasons.get(requestId);
        cancelHostRoleCommands(tasksToDequeue, reason);
        List<Stage> stages = db.getAllStages(requestId);
        for (Stage stage : stages) {
          abortOperationsForStage(stage);
        }
      }
      requestsToBeCancelled.clear();
      requestCancelReasons.clear();
    }
  }

  /**
   * Cancels host role commands (those that are not finished yet).
   * Dequeues host role commands that have been added to ActionQueue,
   * and automatically generates and adds to ActionQueue CANCEL_COMMANDs
   * for all hostRoleCommands that have already been sent to an agent for
   * execution.
   * @param hostRoleCommands a list of hostRoleCommands
   * @param reason why the request is being cancelled
   */
  void cancelHostRoleCommands(Collection<HostRoleCommand> hostRoleCommands, String reason) {
    for (HostRoleCommand hostRoleCommand : hostRoleCommands) {
      if (hostRoleCommand.getStatus() == HostRoleStatus.QUEUED) {
        // Dequeue all tasks that have been already scheduled for sending to agent
        actionQueue.dequeue(hostRoleCommand.getHostName(),
                hostRoleCommand.getExecutionCommandWrapper().
                        getExecutionCommand().getCommandId());
      }
      if (hostRoleCommand.getStatus() == HostRoleStatus.QUEUED ||
            hostRoleCommand.getStatus() == HostRoleStatus.IN_PROGRESS) {
        CancelCommand cancelCommand = new CancelCommand();
        cancelCommand.setTargetTaskId(hostRoleCommand.getTaskId());
        cancelCommand.setReason(reason);
        actionQueue.enqueue(hostRoleCommand.getHostName(), cancelCommand);
      }

      if (hostRoleCommand.getStatus().isHoldingState()) {
        db.abortHostRole(hostRoleCommand.getHostName(),
            hostRoleCommand.getRequestId(),
            hostRoleCommand.getStageId(), hostRoleCommand.getRole().name());
      }

      // If host role is an Action, we have to send an event
      if (hostRoleCommand.getRoleCommand().equals(RoleCommand.ACTIONEXECUTE)) {
        String clusterName = hostRoleCommand.getExecutionCommandWrapper().getExecutionCommand().getClusterName();
        processActionDeath(clusterName,
                hostRoleCommand.getHostName(),
                hostRoleCommand.getRole().name());
      }
    }
  }


  /**
   * Attempts to process kill/timeout/abort of action and send
   * appropriate event to all listeners
   */
  private void processActionDeath(String clusterName,
                                  String hostname,
                                  String role) {
    try {
      // Usually clusterId is defined (except the awkward case when
      // "Distribute repositories/install packages" action has been issued
      // against a concrete host without binding to a cluster)
      Long clusterId = clusterName != null ?
              clusters.getCluster(clusterName).getClusterId() : null;
      ActionFinalReportReceivedEvent event = new ActionFinalReportReceivedEvent(
              clusterId, hostname, null,
              role);
      ambariEventPublisher.publish(event);
    } catch (AmbariException e) {
      LOG.error(String.format("Can not get cluster %s", clusterName), e);
    }
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
      case HOLDING:
      case HOLDING_FAILED:
      case HOLDING_TIMEDOUT:
        rs.numHolding++;
        break;
      case SKIPPED_FAILED:
        rs.numSkipped++;
        break;
      default:
        LOG.error("Unknown status " + status.name());
    }
  }


  public void setTaskTimeoutAdjustment(boolean val) {
    taskTimeoutAdjustment = val;
  }

  ServerActionExecutor getServerActionExecutor() {
    return serverActionExecutor;
  }

  static class RoleStats {
    int numInProgress;
    int numQueued = 0;
    int numSucceeded = 0;
    int numFailed = 0;
    int numTimedOut = 0;
    int numPending = 0;
    int numAborted = 0;
    int numHolding = 0;
    int numSkipped = 0;

    final int totalHosts;
    final float successFactor;

    RoleStats(int total, float successFactor) {
      totalHosts = total;
      this.successFactor = successFactor;
    }

    /**
     * Role successful means the role is successful enough to
     */
    boolean isSuccessFactorMet() {
      int minSuccessNeeded = (int) Math.ceil(successFactor * totalHosts);
      return minSuccessNeeded <= numSucceeded;
    }

    private boolean isRoleInProgress() {
      return numPending + numQueued + numInProgress + numHolding > 0;
    }

    /**
     * Role failure means role is no longer in progress and success factor is
     * not met.
     */
    boolean isRoleFailed() {
      return !(isRoleInProgress() || isSuccessFactorMet());
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("numQueued=").append(numQueued);
      builder.append(", numInProgress=").append(numInProgress);
      builder.append(", numSucceeded=").append(numSucceeded);
      builder.append(", numFailed=").append(numFailed);
      builder.append(", numTimedOut=").append(numTimedOut);
      builder.append(", numPending=").append(numPending);
      builder.append(", numAborted=").append(numAborted);
      builder.append(", numSkipped=").append(numSkipped);
      builder.append(", totalHosts=").append(totalHosts);
      builder.append(", successFactor=").append(successFactor);
      return builder.toString();
    }
  }
}
