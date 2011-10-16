/*
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

package org.apache.hms.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hms.common.conf.CommonConfigurationKeys;
import org.apache.hms.common.entity.Status;
import org.apache.hms.common.entity.action.Action;
import org.apache.hms.common.entity.action.ActionDependency;
import org.apache.hms.common.entity.action.ActionStatus;
import org.apache.hms.common.entity.action.PackageAction;
import org.apache.hms.common.entity.cluster.MachineState;
import org.apache.hms.common.entity.cluster.MachineState.StateEntry;
import org.apache.hms.common.entity.command.ClusterCommand;
import org.apache.hms.common.entity.command.Command;
import org.apache.hms.common.entity.command.CommandStatus;
import org.apache.hms.common.entity.command.CreateClusterCommand;
import org.apache.hms.common.entity.command.DeleteClusterCommand;
import org.apache.hms.common.entity.command.UpgradeClusterCommand;
import org.apache.hms.common.entity.command.CommandStatus.ActionEntry;
import org.apache.hms.common.entity.command.CommandStatus.HostStatusPair;
import org.apache.hms.common.entity.command.DeleteCommand;
import org.apache.hms.common.entity.manifest.ClusterHistory;
import org.apache.hms.common.entity.manifest.ClusterManifest;
import org.apache.hms.common.entity.manifest.ConfigManifest;
import org.apache.hms.common.entity.manifest.NodesManifest;
import org.apache.hms.common.entity.manifest.PackageInfo;
import org.apache.hms.common.entity.manifest.Role;
import org.apache.hms.common.entity.manifest.SoftwareManifest;
import org.apache.hms.common.util.ExceptionUtil;
import org.apache.hms.common.util.JAXBUtil;
import org.apache.hms.common.util.ZookeeperUtil;
import org.apache.zookeeper.AsyncCallback.Children2Callback;
import org.apache.zookeeper.AsyncCallback.VoidCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;


public class CommandHandler implements Children2Callback, VoidCallback, Watcher {
  private static Log LOG = LogFactory.getLog(CommandHandler.class);
  private static long SEVEN_DAYS = 1000L*60*1440*7;
  private static String AGENT_ACTION = "/action";
  private static String AGENT_STATUS = "/status";
  private static String AGENT_WORKLOG = "/worklog";
  public static String COMMAND_STATUS = "/status";
  private static AtomicInteger actionCount = new AtomicInteger();
  
  private final ZooKeeper zk;
  private final int handlerCount;
  private final LinkedBlockingQueue<String> tasks = new LinkedBlockingQueue<String>();
  // access to watchedMachineNodes needs to be synchronized on itself
  private final Map<String, Set<String>> watchedMachineNodes = new HashMap<String, Set<String>>();
  private Handler handlers[];
  private volatile boolean running = true; // true while controller runs
  
  public CommandHandler(ZooKeeper zk, int handlerCount) throws KeeperException, InterruptedException {
    this.zk = zk;
    this.handlerCount = handlerCount;
    zk.getChildren(CommonConfigurationKeys.ZOOKEEPER_COMMAND_QUEUE_PATH_DEFAULT, this);
    zk.getChildren(CommonConfigurationKeys.ZOOKEEPER_LIVE_CONTROLLER_PATH_DEFAULT, this);
    zk.getChildren(CommonConfigurationKeys.ZOOKEEPER_STATUS_QUEUE_PATH_DEFAULT, this);
  }

  @Override
  public void processResult(int rc, String path, Object ctx) {
  }
  
  @Override
  public void processResult(int rc, String path, Object ctx,
      List<String> children, Stat stat) {
    for (String child : children) {
      tasks.add(path + "/" + child);
    }
  }
  
  @Override
  public void process(WatchedEvent event) {
    String path = event.getPath();
    LOG.info("Triggered path: "+path);
    if (event.getType() == Event.EventType.NodeChildrenChanged) {
      if (path.equals(CommonConfigurationKeys.ZOOKEEPER_LIVE_CONTROLLER_PATH_DEFAULT)) {
        zk.getChildren(CommonConfigurationKeys.ZOOKEEPER_COMMAND_QUEUE_PATH_DEFAULT, this, this, null);
      } else {
        zk.getChildren(path, this, this, null);
      }
    } else if (event.getType() == Event.EventType.NodeDataChanged) {
      tasks.add(path);
    }
  }
  
  private boolean isMachineNode(String path) {
    if (path.startsWith(CommonConfigurationKeys.ZOOKEEPER_CLUSTER_ROOT_DEFAULT)
        && path.split("/").length == 4) {
      return true;
    }
    return false;
  }
  
  private void checkCmds(String taskPath) throws IOException, KeeperException,
      InterruptedException {
    Set<String> cmdStatusPaths = null;
    synchronized (watchedMachineNodes) {
      cmdStatusPaths = watchedMachineNodes.remove(taskPath);
    }
    /*
     * current controller must already own the locks to these cmds. Otherwise,
     * these cmds wouldn't have been put into watchedMachineNodes.
     */
    if (cmdStatusPaths != null) {
      for (String path : cmdStatusPaths) {
        queueActions(path);
      }
    }
  }
  
  private void handle() throws InterruptedException, KeeperException,
      IOException {
    boolean workOnIt = true;
    String taskPath = tasks.take();
    if (isMachineNode(taskPath)) {
      // machine state change
      LOG.info("machine state changed: " + taskPath);
      checkCmds(taskPath);
      return;
    }
    try {
      // trying to acquire the lock
      zk.create(CommonConfigurationKeys.ZOOKEEPER_LOCK_QUEUE_PATH_DEFAULT + "/"
          + taskPath.replace('/', '.'), new byte[0], Ids.OPEN_ACL_UNSAFE,
          CreateMode.EPHEMERAL);
    } catch (KeeperException.NodeExistsException e) {
      // client cmd or action status has been processed
      return;
    }
    // got the lock
    if (taskPath
        .startsWith(CommonConfigurationKeys.ZOOKEEPER_COMMAND_QUEUE_PATH_DEFAULT)) {
      String cmd = taskPath.substring(taskPath.lastIndexOf('/') + 1);
      if (cmd.indexOf('-') < 0) {
        throw new IOException("Unknown command: " + cmd);
      }
      byte[] data = zk.getData(taskPath, false, null);
      Command command = JAXBUtil.read(data, Command.class);
      String commandStatusPath = ZookeeperUtil.getCommandStatusPath(taskPath);
      Stat stat = zk.exists(commandStatusPath, false);
      if(stat!=null) {
        byte[] test = zk.getData(commandStatusPath, false, stat);
        CommandStatus status = JAXBUtil.read(test, CommandStatus.class);
        if(status.getStatus()==Status.SUCCEEDED || status.getStatus()==Status.FAILED) {
          workOnIt=false;
          // Delete command, if it has been completed and older than
          // 7 days
          if(System.currentTimeMillis()>stat.getMtime()+(SEVEN_DAYS)) {
            LOG.info("Clean up deployment history: "+taskPath);
            deleteIfExists(taskPath);
          }
        }
      }
      if(workOnIt) {
        try {
          if (command instanceof DeleteCommand) {
            deleteCluster(taskPath, (DeleteCommand) command);
          } else if (command instanceof CreateClusterCommand) {
            createCluster(taskPath, (CreateClusterCommand) command);
          } else if (command instanceof DeleteClusterCommand) {
            deleteCluster(taskPath, (DeleteClusterCommand) command);
          } else if (command instanceof UpgradeClusterCommand) {
            updateCluster(taskPath, (ClusterCommand) command);
          } else {
            throw new IOException("Unknown command: " + command);
          }
        } catch(KeeperException e) {
          unlockCommand(taskPath);          
          // Look for other command to work.
          zk.getChildren(CommonConfigurationKeys.ZOOKEEPER_COMMAND_QUEUE_PATH_DEFAULT, this);
        }
      } else {
        unlockCommand(taskPath);
        // Look for other command to work.
        zk.getChildren(CommonConfigurationKeys.ZOOKEEPER_COMMAND_QUEUE_PATH_DEFAULT, this);
      }
    } else if (taskPath
        .startsWith(CommonConfigurationKeys.ZOOKEEPER_STATUS_QUEUE_PATH_DEFAULT)) {
      updateSystemState(taskPath);
    } else if (taskPath
        .startsWith(CommonConfigurationKeys.ZOOKEEPER_CLUSTER_ROOT_DEFAULT)) {
      String queueNode = taskPath.substring(0, taskPath.lastIndexOf('/'));
      LOG.info("queueNode is " + queueNode);
      if (queueNode.endsWith(AGENT_STATUS)) {
        // agent status event
        updateSystemState(taskPath);
      } else if (queueNode.endsWith(AGENT_ACTION)) {
        // action being queued
        runFakeAgent(taskPath);
      } else {
        throw new IOException("Unknown event: " + taskPath);
      }
    } else {
      throw new IOException("Unexpected request: " + taskPath);
    }
  }
  
  /**
   * Simulate Agent Status
   */
  private void runFakeAgent(String actionPath) throws KeeperException,
      InterruptedException, IOException {
    LOG.info("Fake agent received action event at " + actionPath);
    Thread.sleep(1000);
    Stat stat = zk.exists(actionPath, false);
    if (stat == null) {
      // action has been worked on
      return;
    }
    Action action = JAXBUtil.read(zk.getData(actionPath, false, null),
        Action.class);
    // create worklog node
    zk.create(actionPath + AGENT_WORKLOG, new byte[0], Ids.OPEN_ACL_UNSAFE,
        CreateMode.PERSISTENT);
    ActionStatus status = new ActionStatus();
    status.setStatus(Status.SUCCEEDED);
    status.setError("Failure is unavoidable");
    status.setCmdPath(action.getCmdPath());
    status.setActionId(action.getActionId());
    status.setActionPath(actionPath);
    String actionQueue = actionPath.substring(0, actionPath.lastIndexOf('/'));
    String hostNode = actionQueue.substring(0, actionQueue.lastIndexOf('/'));
    status.setHost(hostNode);
    String statusNode = zk.create(hostNode + AGENT_STATUS + "/" + "status-",
        JAXBUtil.write(status), Ids.OPEN_ACL_UNSAFE,
        CreateMode.PERSISTENT_SEQUENTIAL);
    LOG.info("Fake agent queued status object at " + statusNode);
  }
  
  private void updateSystemState(String statusPath)
      throws InterruptedException, KeeperException, IOException {
    LOG.info("status path is: " + statusPath);
    Stat stat = zk.exists(statusPath, false);
    if (stat == null) {
      /* status has been previously processed by either this or another controller
       * delete the status lock if it exists 
       */
      LOG.info("status has been previously processed: " + statusPath);
      statusCleanup(statusPath, null);
      return;
    }
    ActionStatus actionStat = JAXBUtil.read(
        zk.getData(statusPath, false, null), ActionStatus.class);
    if (actionStat.getStatus() != Status.SUCCEEDED
        && actionStat.getStatus() != Status.FAILED)
      throw new IOException("Invalid action status: " + actionStat.getStatus()
          + " from action " + actionStat.getActionPath());
    String actionPath = actionStat.getActionPath();
    stat = zk.exists(actionPath, false);
    if (stat == null) {
      /* status has been previously processed by either this or another controller
       * delete the status znode, plus action and status locks 
       */
      statusCleanup(statusPath, actionPath);
      return;
    }
    
    String actionQueue = actionPath.substring(0, actionPath.lastIndexOf('/'));
    String hostNode = actionQueue.substring(0, actionQueue.lastIndexOf('/'));
    // update system status
    if (actionStat.getStatus() == Status.SUCCEEDED) {
      Action action = JAXBUtil.read(zk.getData(actionPath, false, null),
          Action.class);
      MachineState machineState = JAXBUtil.read(zk.getData(hostNode, false, stat),
          MachineState.class);
      boolean retry = true;
      while (retry) {
        retry = false;
        Set<StateEntry> states = machineState.getStates();
        if (states == null) {
          states = new HashSet<StateEntry>();
        }
        if(action.getExpectedResults()!=null) {
          states.addAll(action.getExpectedResults());
        }
        machineState.setStates(states);
        try {
          stat = zk.setData(hostNode, JAXBUtil.write(machineState), stat
              .getVersion());
        } catch (KeeperException.BadVersionException e) {
          LOG.info("version mismatch: expected=" + stat.getVersion() + " msg: "
              + e.getMessage());
          machineState = JAXBUtil.read(zk.getData(hostNode, false, stat),
              MachineState.class);
          LOG.info("new version is " + stat.getVersion());
          retry = true;
        }
      }
    }
    
    // update cmd status
    if (actionStat.getStatus() != Status.SUCCEEDED) {
      try {
        zk.create(actionStat.getCmdPath() + COMMAND_STATUS + "/"
            + actionStat.getHost().replace('/', '.') + "-"
            + actionStat.getActionId(), JAXBUtil.write(actionStat),
            Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
      } catch (KeeperException.NodeExistsException e) {
      }
    }

    String host = hostNode.substring(hostNode.lastIndexOf('/') + 1);
    String cmdStatusPath = actionStat.getCmdPath() + COMMAND_STATUS;
    CommandStatus cmdStatus = JAXBUtil.read(zk.getData(cmdStatusPath, false,
        stat), CommandStatus.class);
    boolean retry = true;
    while (retry) {
      retry = false;
      boolean found = false;
      boolean needUpdate = false;
      for (ActionEntry actionEntry : cmdStatus.getActionEntries()) {
        if (actionEntry.getAction().getActionId() == actionStat.getActionId()) {
          int failCount = 0;
          if(actionStat.getStatus()==Status.FAILED) {
            // Count current action status if it has failed.
            failCount++;
          }
          for (HostStatusPair hsp : actionEntry.getHostStatus()) {
            if(hsp.getStatus()==Status.FAILED) {
              // Walk through existing hosts, and count number of failed
              // actions.
              failCount++;
            }
            if (host.equals(hsp.getHost())) {
              found = true;
              Status status = hsp.getStatus();
              if (status == Status.UNQUEUED || status == Status.QUEUED
                  || status == Status.STARTED) {
                hsp.setStatus(actionStat.getStatus());
                cmdStatus.setCompletedActions(cmdStatus.getCompletedActions() + 1);                
                if (cmdStatus.getCompletedActions() == cmdStatus.getTotalActions()) {
                  Status overallStatus = Status.SUCCEEDED;
                  for (ActionEntry aEntry : cmdStatus.getActionEntries()) {
                    boolean shouldBreak = false;
                    for (HostStatusPair hspair : aEntry.getHostStatus()) {
                      if (hspair.getStatus() != Status.SUCCEEDED) {
                        overallStatus = Status.FAILED;
                        shouldBreak = true;
                        break;
                      }
                    }
                    if (shouldBreak)
                      break;
                  }
                  cmdStatus.setStatus(overallStatus);
                  cmdStatus.setEndTime(new Date(System.currentTimeMillis()).toString());
                  updateClusterStatus(actionStat.getCmdPath());
                } else if(failCount==actionEntry.getHostStatus().size()) {
                  // If all nodes failed the action, set the command to fail.
                  cmdStatus.setStatus(Status.FAILED);
                  cmdStatus.setEndTime(new Date(System.currentTimeMillis()).toString());
                  updateClusterStatus(actionStat.getCmdPath());
                }
                needUpdate = true;
                LOG.info("Fail count:"+failCount);
                break;
              } else if (status == actionStat.getStatus()) {
                // duplicate status update, nothing to be done
              } else {
                throw new IOException("UNEXPECTED action status: " + actionStat.getStatus()
                    + " from action " + actionPath + ", current host status is " + status);
              }
            }
          }
          if (found) {
            break;
          }
        }
      }
      if (!found) {
        throw new IOException("UNEXPECTED: can't find action " + actionPath);
      }
      if (needUpdate) {
        try {
          stat = zk.setData(cmdStatusPath, JAXBUtil.write(cmdStatus), stat
              .getVersion());
          if(cmdStatus.getStatus() == Status.SUCCEEDED || cmdStatus.getStatus() == Status.FAILED) {
            unlockCommand(actionStat.getCmdPath());
          }
        } catch (KeeperException.BadVersionException e) {
          LOG.info("version mismatch: expected=" + stat.getVersion() + " msg: "
              + e.getMessage());
          cmdStatus = JAXBUtil.read(zk.getData(cmdStatusPath, false, stat),
              CommandStatus.class);
          LOG.info("new version is " + stat.getVersion());
          retry = true;
        }
      }
    }

    statusCleanup(statusPath, actionPath);
    LOG.info("Deleted action:" + actionPath + ", status:" + statusPath);
  }

  public void unlockCommand(String cmdPath) {
    String cmdLock = cmdPath.replace('/', '.');
    try {
      deleteIfExists(CommonConfigurationKeys.ZOOKEEPER_LOCK_QUEUE_PATH_DEFAULT+"/"+cmdLock);
    } catch (InterruptedException e) {
      LOG.warn("Unable to unlock:" + cmdPath);
    } catch (KeeperException e) {
      LOG.warn("Unable to unlock:" + cmdPath);
    }
  }
  
  private void updateClusterStatus(String cmdPath) throws IOException, KeeperException, InterruptedException {
    Stat current = zk.exists(cmdPath, false);
    ClusterCommand cmd = JAXBUtil.read(zk.getData(cmdPath, false, current), ClusterCommand.class);
    String clusterPath = ZookeeperUtil.getClusterPath(cmd.getClusterManifest().getClusterName());
    boolean retry = true;
    while(retry) {
      retry = false;
      try {
        if(cmd instanceof DeleteClusterCommand) {
          deleteIfExists(clusterPath);
        }
        unlockCluster(cmd.getClusterManifest().getClusterName());
      } catch(KeeperException.BadVersionException e) {
        retry = true;
        LOG.warn(ExceptionUtil.getStackTrace(e));
        LOG.warn("version mismatch: expected=" + current.getVersion());
        zk.getData(clusterPath, false, current);
        LOG.warn("Cluster status update failed.  Cluster ID:"+cmd.getClusterManifest().getClusterName()+" state: "+ current.getVersion());
      }
    }
  }
  
  private void statusCleanup(String statusPath, String actionPath)
      throws InterruptedException, KeeperException {
    deleteIfExists(actionPath);
    deleteIfExists(statusPath);
    // delete action lock for fake agent
    if (actionPath != null && actionPath.length() > 0) {
      deleteIfExists(CommonConfigurationKeys.ZOOKEEPER_LOCK_QUEUE_PATH_DEFAULT
          + "/" + actionPath.replace('/', '.'));
    }
    // delete status lock
    if (statusPath != null && statusPath.length() > 0) {
      deleteIfExists(CommonConfigurationKeys.ZOOKEEPER_LOCK_QUEUE_PATH_DEFAULT
          + "/" + statusPath.replace('/', '.'));
    }
  }
  
  private void deleteIfExists(String path) throws InterruptedException,
      KeeperException {
    if (path == null || path.length() == 0) {
      return;
    }
    Stat stat = zk.exists(path, false);
    if (stat == null) {
      return;
    }
    List<String> children = null;
    try {
      children = zk.getChildren(path, null);
      if (children != null) {
        for (String child : children) {
          deleteIfExists(path + "/" + child);
        }
      }
      zk.delete(path, -1);
    } catch (KeeperException.NoNodeException e) {
      LOG.info(ExceptionUtil.getStackTrace(e));
    }
  }

  private void createNodeIfNecessary(String path, byte[] data) throws KeeperException,
      InterruptedException {
    try {
      zk.create(path, data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    } catch (KeeperException.NodeExistsException e) {
    }
  }
  
  private void createAndWatchClusterNodes(ClusterManifest cm, String cmdStatusPath)
    throws KeeperException, InterruptedException, IOException  {
    CommandStatus cmdStatus = JAXBUtil.read(zk.getData(cmdStatusPath, false, null), CommandStatus.class);
    List<ActionEntry> list = cmdStatus.getActionEntries();
    HashSet<String> hosts = new HashSet<String>();
    for(ActionEntry a : list) {
      List<HostStatusPair> hsList = a.getHostStatus();
      for(HostStatusPair hsp : hsList) {
        hosts.add(hsp.getHost());
      }
    }
    ClusterHistory history;
    try {
      String path = ZookeeperUtil.getClusterPath(cm.getClusterName());
      Stat stat = zk.exists(path, false);
      byte[] buffer = zk.getData(path, false, stat);
      history = JAXBUtil.read(buffer, ClusterHistory.class);
    } catch(KeeperException.NoNodeException e) {
      history = new ClusterHistory();
      ArrayList<ClusterManifest> manifests = new ArrayList<ClusterManifest>();
      manifests.add(cm);
      history.setHistory(manifests);      
    }
    createAndWatchClusterNodes(cmdStatus.getClusterName(), hosts, history);
  }
  
  private void createAndWatchClusterNodes(String cluster , Set<String> hosts, ClusterHistory ch) throws KeeperException, InterruptedException, IOException {
    byte[] empty = new byte[0];
    String clusterNode = ZookeeperUtil.getClusterPath(cluster);
    createNodeIfNecessary(clusterNode, JAXBUtil.write(ch));
    // create host nodes and queues
    for (String host : hosts) {
      String hostNode = clusterNode + "/" + host;
      createNodeIfNecessary(hostNode, JAXBUtil.write(new MachineState()));
      String actionQueue = hostNode + AGENT_ACTION;
      createNodeIfNecessary(actionQueue, empty);
      String statusQueue = hostNode + AGENT_STATUS;
      createNodeIfNecessary(statusQueue, empty);
      // watch on agent status queue
      zk.getChildren(statusQueue, this);
      // to run fake agent, watch on action queue also
      //zk.getChildren(actionQueue, this);
    }
  }
  

  private void commitCommandPlan(String cmdStatusPath, CommandStatus cmdStatus, List<ActionEntry> actionEntries) throws KeeperException, InterruptedException, IOException {
    cmdStatus.setActionEntries(actionEntries);
    int totalActions = 0;
    for (ActionEntry a : actionEntries) {
      totalActions += a.getHostStatus().size();
    }
    cmdStatus.setTotalActions(totalActions);
    // write out the plan that is captured in cmdStatus
    zk.create(cmdStatusPath, JAXBUtil.write(cmdStatus), Ids.OPEN_ACL_UNSAFE,
        CreateMode.PERSISTENT);
  }
  
  private Set<String> convertRolesToHosts(NodesManifest nodesManifest, Set<String> roles) {
    Set<String> hosts = new HashSet<String>();
    if (roles==null) {
      // If roles are unspecified, expand the unique host list
      for(Role collectRole : nodesManifest.getRoles()) {
        String[] hostList = collectRole.getHosts();
        for(String host : hostList) {
          hosts.add(host);
        }
      }
    } else {
      for(String role : roles) {
        for(Role testRole : nodesManifest.getRoles()) {
          if(role.equals(testRole.getName())) {
            String[] hostList = testRole.getHosts();
            for(String host : hostList) {
              hosts.add(host);
            }
          }
        }
      }
    }
    return hosts;
  }
  
  private PackageInfo[] convertRolesToPackages(SoftwareManifest softwareManifest, String role) {
    Set<PackageInfo> packages = new LinkedHashSet<PackageInfo>();
    for(Role tmp : softwareManifest.getRoles()) {
      if(role==null || tmp.equals(role)) {
        for(PackageInfo p : tmp.getPackages()) {
          packages.add(p);
        }
      }
    }
    return packages.toArray(new PackageInfo[packages.size()]);
  }
  
  private List<HostStatusPair> setHostStatus(Set<String> hosts, Status status) {
    List<HostStatusPair> nodesList = new ArrayList<HostStatusPair>();
    for(String node : hosts) {
      HostStatusPair hsp = new HostStatusPair(node, status);
      nodesList.add(hsp);
    }
    return nodesList;
  }
  
  /**
   * Create a lock for serializing cluster related commands.
   * @param clusterName
   * @throws KeeperException
   * @throws InterruptedException
   * @throws IOException
   */
  private void lockCluster(String taskPath, String clusterName) throws KeeperException, InterruptedException, IOException {
    StringBuilder path = new StringBuilder();
    path.append(CommonConfigurationKeys.ZOOKEEPER_LOCK_QUEUE_PATH_DEFAULT);
    path.append("/");
    path.append("cluster.");
    path.append(clusterName);
    try {
    zk.create(path.toString(), new byte[0], Ids.OPEN_ACL_UNSAFE,
        CreateMode.EPHEMERAL);
    } catch(KeeperException e) {
      tasks.add(taskPath);
      throw e;
    }
  }

  /**
   * Unlock cluster lock for operating next cluster related commands.
   * @param clusterName
   * @return Stat which cluster is unlocked
   * @throws KeeperException
   * @throws InterruptedException
   * @throws IOException
   */
  public void unlockCluster(String clusterName) throws KeeperException, InterruptedException, IOException {
    try {
      StringBuilder path = new StringBuilder();
      path.append(CommonConfigurationKeys.ZOOKEEPER_LOCK_QUEUE_PATH_DEFAULT);
      path.append("/");
      path.append("cluster.");
      path.append(clusterName);
      Stat stat = zk.exists(path.toString(), false);
      zk.delete(path.toString(), stat.getVersion());
    } catch(KeeperException.NoNodeException e) {      
    }
  }

  /**
   * Check all cluster for duplicated nodes in use.
   * @param nm
   * @return true if node is already used by another cluster.
   * @throws InterruptedException 
   * @throws KeeperException 
   * @throws IOException 
   */
  private boolean checkNodesInUse(NodesManifest nm) throws KeeperException, InterruptedException {
    Set<String> hosts = convertRolesToHosts(nm, null);
    List<String> children = zk.getChildren(CommonConfigurationKeys.ZOOKEEPER_CLUSTER_ROOT_DEFAULT, null);
    Stat stat = new Stat();
    boolean result = false;
    for(String cluster : children) {
      try {
        LOG.info("Check "+cluster);
        String path = ZookeeperUtil.getClusterPath(cluster);
        byte[] data = zk.getData(path, false, stat);
        ClusterHistory ch = JAXBUtil.read(data, ClusterHistory.class);
        int index = ch.getHistory().size() - 1;
        ClusterManifest cm = ch.getHistory().get(index);
        Set<String> test = convertRolesToHosts(cm.getNodes(), null);
        hosts.retainAll(test);
        if(!hosts.isEmpty()) {
          result = true;
          break;
        }
      } catch(Exception e) {
        LOG.error(ExceptionUtil.getStackTrace(e));
      }
    }
    return result;
  }
  
  /**
   * Update Zookeeper for failed command status
   * @param path
   * @param cmd
   * @throws IOException
   * @throws KeeperException
   * @throws InterruptedException
   */
  public void failCommand(String path, Command cmd) throws IOException, KeeperException, InterruptedException {
    String cmdStatusPath = ZookeeperUtil.getCommandStatusPath(path);
    CommandStatus cmdStatus = new CommandStatus();
    String currentTime = new Date(System.currentTimeMillis()).toString();
    cmdStatus.setStartTime(currentTime);
    cmdStatus.setEndTime(currentTime);
    cmdStatus.setStatus(Status.FAILED);
    cmdStatus.setTotalActions(0);
    boolean retry = true;
    while(retry) {
      try {
        Stat stat = zk.exists(cmdStatusPath, false);
        if(stat==null) {
          zk.create(cmdStatusPath, JAXBUtil.write(cmdStatus), Ids.OPEN_ACL_UNSAFE,
          CreateMode.PERSISTENT);
        } else {
          zk.setData(cmdStatusPath, JAXBUtil.write(cmdStatus), stat.getVersion());
        }
        retry = false;
      } catch(KeeperException.BadVersionException e) {
        retry = true;
      }
    }
    if(cmd instanceof ClusterCommand ) {
      try {
        String clusterName = ((ClusterCommand) cmd).getClusterManifest().getClusterName();
        unlockCluster(clusterName);
      } catch(NullPointerException e) {
        // Ignore if the cluster has not been locked.
      }
    }
    String cmdPath = CommonConfigurationKeys.ZOOKEEPER_COMMAND_QUEUE_PATH_DEFAULT + "/" + cmd.getId();
    unlockCommand(cmdPath);
  }
  
  private void createCluster(String taskPath, ClusterCommand command) throws KeeperException, InterruptedException, IOException {
    lockCluster(taskPath, command.getClusterManifest().getClusterName());
    if(checkNodesInUse(command.getClusterManifest().getNodes())) {
      failCommand(taskPath, command);
      LOG.error("Duplicated nodes detected in existing cluster.");
      unlockCluster(command.getClusterManifest().getClusterName());
      return;
    }
    generateClusterPlan(taskPath, (ClusterCommand) command);
    String cmdStatusPath = ZookeeperUtil.getCommandStatusPath(taskPath);
    runClusterActions(command.getClusterManifest(), cmdStatusPath);
  }

  private void updateCluster(String taskPath, ClusterCommand command) throws KeeperException, InterruptedException, IOException {
    lockCluster(taskPath, command.getClusterManifest().getClusterName());
    generateClusterPlan(taskPath, (ClusterCommand) command);
    String cmdStatusPath = ZookeeperUtil.getCommandStatusPath(taskPath);
    runClusterActions(command.getClusterManifest(), cmdStatusPath);
  }

  private void deleteCluster(String taskPath, DeleteClusterCommand command) throws KeeperException, InterruptedException, IOException {
    lockCluster(taskPath, command.getClusterManifest().getClusterName());
    ClusterManifest cm = command.getClusterManifest();
    String path = ZookeeperUtil.getClusterPath(cm.getClusterName());
    byte[] data = zk.getData(path, null, null);
    ClusterHistory history = JAXBUtil.read(data, ClusterHistory.class);
    int index = history.getHistory().size()-1;
    ClusterManifest currentCluster = history.getHistory().get(index);
    cm.setNodes(currentCluster.getNodes());
    generateClusterPlan(taskPath, (ClusterCommand) command);
    String cmdStatusPath = ZookeeperUtil.getCommandStatusPath(taskPath);
    runClusterActions(command.getClusterManifest(), cmdStatusPath);
  }
  
  private void generateClusterPlan(String cmdPath, ClusterCommand cmd) throws KeeperException, InterruptedException, IOException {
    String cmdStatusPath = ZookeeperUtil.getCommandStatusPath(cmdPath);
    Stat stat = zk.exists(cmdStatusPath, false);
    if (stat != null) {
      // plan already exists, let's pick up what's left from another controller
      return;
    }
    // new create command
    LOG.info("Generate command plan: " + cmdPath);
    String startTime = new Date(System.currentTimeMillis()).toString();
    CommandStatus cmdStatus = new CommandStatus(Status.STARTED, startTime);
    
    // Setup actions
    List<ActionEntry> actionEntries = new LinkedList<ActionEntry>();

    ClusterManifest cm = ((ClusterCommand) cmd).getClusterManifest();
    cmdStatus.setClusterName(cm.getClusterName());
    NodesManifest nm = cm.getNodes();
    ConfigManifest configM = cm.getConfig();
    for(Action action : configM.getActions()) {
      // Find the host list for this action
      Set<String> hosts;
      if(action.getRole()==null) {
        hosts = convertRolesToHosts(nm, null);
      } else {
        Set<String> role = new HashSet<String>();
        role.add(action.getRole());
        hosts = convertRolesToHosts(nm, role);
      }
      List<HostStatusPair> nodesList = setHostStatus(hosts, Status.UNQUEUED);

      ActionEntry ae = new ActionEntry();
      action.setCmdPath(cmdPath);
      action.setActionId(actionCount.incrementAndGet());
      ae.setHostStatus(nodesList);
      List<ActionDependency> adList = action.getDependencies();
      if(adList!=null) {
        for(ActionDependency ad : adList) {
          Set<String> roles = ad.getRoles();
          Set<String> dependentHosts = convertRolesToHosts(nm, roles);
          StringBuilder sb = new StringBuilder();
          List<String> myhosts = new ArrayList<String>();
          for(String host : dependentHosts) {
            sb.append(CommonConfigurationKeys.ZOOKEEPER_CLUSTER_ROOT_DEFAULT);
            sb.append("/");
            sb.append(cm.getClusterName());
            sb.append("/");
            sb.append(host);
            myhosts.add(sb.toString());
            sb.delete(0, sb.length());
          }
          ad.setHosts(myhosts);
        }
      }

      // If the action is package action resolve the action from software manifest
      if(action instanceof PackageAction) {
        SoftwareManifest sm = cm.getSoftware();
        if(action.getRole()==null) {
          // If no role is defined, install all the software in the software manifest
          PackageInfo[] packages = convertRolesToPackages(sm, null);
          ((PackageAction) action).setPackages(packages);
        } else {
          for(Role role : sm.getRoles()) {
            if(role.getName().equals(action.getRole())) {
              PackageInfo[] packages = convertRolesToPackages(sm, action.getRole());
              ((PackageAction) action).setPackages(packages);
            }
          }       
        }
      }
        
      ae.setAction(action);
      actionEntries.add(ae);
    }
    commitCommandPlan(cmdStatusPath, cmdStatus, actionEntries);    
  }
  
  private void runClusterActions(ClusterManifest cm, String cmdStatusPath) throws KeeperException, InterruptedException, IOException {
    CommandStatus cmdStatus = JAXBUtil.read(zk.getData(cmdStatusPath, false, null), CommandStatus.class);
    String cluster = cmdStatus.getClusterName();
    String clusterNode = ZookeeperUtil.getClusterPath(cluster);
    try {
      createAndWatchClusterNodes(cm, cmdStatusPath);
    } catch(KeeperException e) {
      LOG.debug(ExceptionUtil.getStackTrace(e));
    }
    queueActions(cmdStatusPath);
    LOG.info("Issued actions for cluster [" + cluster
        + "] with " + zk.exists(clusterNode, null).getNumChildren()
        + " cluster nodes");
    return;
  }
  
  private boolean isDependencySatisfied(String cmdStatusPath,
      List<ActionDependency> dependencies) throws KeeperException,
      InterruptedException, IOException {
    if (dependencies == null) {
      return true;
    }
    Stat stat = new Stat();
    for (ActionDependency dep : dependencies) {
      List<String> hosts = dep.getHosts();
      List<StateEntry> deps = dep.getStates();
      if (hosts == null || hosts.size() == 0 || deps == null
          || deps.size() == 0) {
        continue;
      }
      int satisfied = 0;
      for (String host : hosts) {
        MachineState state = JAXBUtil.read(zk.getData(host, this, stat),
            MachineState.class);
        if (state == null || state.getStates() == null
            || !state.getStates().containsAll(deps)) {
          /*
           * Adding the cmd to watchedMachineNodes and return. We only add the
           * cmd once. Note that whenever the watch is triggered we remove the
           * mapping from watchedMachineNodes. This ensures that at any time
           * there is at most one mapping in watchedMachineNodes containing this
           * cmd as its value. Hence, at any time there will be at most one
           * handler thread executing queueActions() for this cmd (i.e., when
           * watch is triggered).
           */
          synchronized (watchedMachineNodes) {
            Set<String> cmdStatusPaths = watchedMachineNodes.get(host);
            if (cmdStatusPaths == null) {
              cmdStatusPaths = new HashSet<String>();
            }
            cmdStatusPaths.add(cmdStatusPath);
            watchedMachineNodes.put(host, cmdStatusPaths);
          }
        } else {
          satisfied++;
        }
      }
      float confidenceLevel = satisfied / hosts.size();
      if(confidenceLevel<0.5) {
        return false;
      }
    }
    return true;
  }
  
  private void queueActions(String cmdStatusPath) throws IOException,
      KeeperException, InterruptedException {
    LOG.info("try to queue actions for cmd " + cmdStatusPath);
    Stat stat = new Stat();
    CommandStatus cmdStatus = JAXBUtil.read(zk.getData(cmdStatusPath, false,
        stat), CommandStatus.class);
    // we queue actions and update their status one at a time. After each
    // action is queued, we try to update its status (retry if necessary).
    // If retry happens, we start over again and try to find actions that
    // need to be issued.
    boolean startOver = true;
    while (startOver) {
      startOver = false;
      for (ActionEntry actionEntry : cmdStatus.getActionEntries()) {
        //TODO needs to check if an actionEntry is already done
        if (!isDependencySatisfied(cmdStatusPath, actionEntry.getAction().getDependencies())) {
          LOG.info("dependency is not satified for actionId=" + actionEntry.getAction().getActionId());
          return;
        }
        int actionId = actionEntry.getAction().getActionId();
        for (HostStatusPair hsp : actionEntry.getHostStatus()) {
          if (hsp.getStatus() == Status.UNQUEUED) {
            // queue action
            String actionNode = CommonConfigurationKeys.ZOOKEEPER_CLUSTER_ROOT_DEFAULT
                + "/"
                + cmdStatus.getClusterName()
                + "/"
                + hsp.getHost()
                + AGENT_ACTION + "/" + "action-";
            actionNode = zk.create(actionNode, JAXBUtil.write(actionEntry
                .getAction()), Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT_SEQUENTIAL);

            // update status for queued action
            String host = hsp.getHost();
            hsp.setStatus(Status.QUEUED);
            boolean retry = true;
            while (retry) {
              retry = false;
              try {
                stat = zk.setData(cmdStatusPath, JAXBUtil.write(cmdStatus),
                    stat.getVersion());
              } catch (KeeperException.BadVersionException e) {
                LOG.info("version mismatch: expected=" + stat.getVersion()
                    + " msg: " + e.getMessage());
                // our copy is stale, we need to start over again after
                // updating the current status
                startOver = true;
                cmdStatus = JAXBUtil.read(zk
                    .getData(cmdStatusPath, false, stat), CommandStatus.class);
                LOG.info("new version is " + stat.getVersion());
                // find the item we want to update and check if it needs to be
                // updated
                boolean found = false;
                for (ActionEntry actEntry : cmdStatus.getActionEntries()) {
                  if (actEntry.getAction().getActionId() == actionId) {
                    for (HostStatusPair hostStat : actEntry.getHostStatus()) {
                      if (hostStat.getHost().equals(host)) {
                        // only update the status when we are in unqueued
                        // state
                        if (hostStat.getStatus() == Status.UNQUEUED) {
                          hostStat.setStatus(Status.QUEUED);
                          retry = true;
                        }
                        found = true;
                        break;
                      }
                    }
                    if (found)
                      break;
                  }
                }
              }
            }
            LOG.info("Queued action " + actionNode);
            if (startOver)
              break;
          }
        }
        if (startOver)
          break;
      }
    }
  }
  
  private void recursiveDelete(String path) throws KeeperException, InterruptedException {
    List<String> children = zk.getChildren(path, null);
    if (children.size() > 0) {
      for (String child : children) {
        recursiveDelete(path + "/" + child);
      }
    }
    zk.delete(path, -1);
  }
  
  private void deleteClusterInZookeeper(String cmdPath, DeleteClusterCommand cmd) 
      throws KeeperException, InterruptedException, IOException {
    String clusterName = cmd.getClusterManifest().getClusterName();
    LOG.info("Starting COMMAND: " + cmd + " on " + cmdPath);
    String cmdStatusPath = cmdPath + COMMAND_STATUS;
    try {
      String startTime = new Date(System.currentTimeMillis()).toString();
      zk.create(cmdStatusPath, JAXBUtil.write(new CommandStatus(Status.STARTED,
          startTime)), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    } catch (KeeperException.NodeExistsException e) {
      // cmd has been worked on
    }
    Stat stat = new Stat();
    byte[] data = zk.getData(cmdStatusPath, false, stat);
    CommandStatus cmdStatus = JAXBUtil.read(data, CommandStatus.class);
    if (cmdStatus.getStatus() == Status.SUCCEEDED) {
      return;
    }
    String clusterPath = CommonConfigurationKeys.ZOOKEEPER_CLUSTER_ROOT_DEFAULT
        + "/" + clusterName;
    deleteIfExists(clusterPath);
    cmdStatus.setEndTime(new Date(System.currentTimeMillis()).toString());
    cmdStatus.setStatus(Status.SUCCEEDED);
    zk.setData(cmdStatusPath, JAXBUtil.write(cmdStatus), stat.getVersion());
    LOG.info("Deleted cluster " + clusterName);    
  }
  
  private void deleteCluster(String cmdPath, DeleteCommand cmd)
      throws KeeperException, InterruptedException, IOException {
    DeleteClusterCommand delete = new DeleteClusterCommand();
    ClusterManifest cm = new ClusterManifest();
    cm.setClusterName(cmd.getClusterName());
    delete.setClusterManifest(cm);
    deleteClusterInZookeeper(cmdPath, delete);
  }
  
  public Command getCommand(String cmdPath) throws KeeperException, InterruptedException, IOException {
    Stat stat = new Stat();
    Command cmd = JAXBUtil.read(zk.getData(cmdPath, false, stat), Command.class);
    return cmd;
  }

  public synchronized void start() {
    handlers = new Handler[handlerCount];
    
    for (int i = 0; i < handlerCount; i++) {
      handlers[i] = new Handler(i);
      handlers[i].start();
    }
  }
  
  /** Stops the service. */
  public synchronized void stop() {
    LOG.info("Stopping command handler");
    running = false;
    if (handlers != null) {
      for (int i = 0; i < handlerCount; i++) {
        if (handlers[i] != null) {
          handlers[i].interrupt();
        }
      }
    }
    notifyAll();
  }

  /** Wait for the server to be stopped.
   * Does not wait for all subthreads to finish.
   *  See {@link #stop()}.
   */
  public synchronized void join() throws InterruptedException {
    while (running) {
      wait();
    }
  }
  
  /** Handles queued commands . */
  private class Handler extends Thread {
    public Handler(int instanceNumber) {
      this.setDaemon(true);
      this.setName("Command handler " + instanceNumber);
    }

    @Override
    public void run() {
      LOG.info(getName() + ": starting");

      while (running) {
        try {
          handle();
        } catch (InterruptedException e) {
          if (running) { // unexpected -- log it
            LOG.warn(getName() + " caught: " + ExceptionUtil.getStackTrace(e));
          }
        } catch (Exception e) {
          LOG.warn(getName() + " caught: " + ExceptionUtil.getStackTrace(e));
        }
      }
      LOG.info(getName() + ": exiting");
    }
  }
}
