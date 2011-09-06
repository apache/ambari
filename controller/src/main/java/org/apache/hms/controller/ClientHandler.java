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

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hms.controller.CommandHandler;
import org.apache.hms.common.conf.CommonConfigurationKeys;
import org.apache.hms.common.entity.cluster.MachineState;
import org.apache.hms.common.entity.command.Command;
import org.apache.hms.common.entity.command.CommandStatus;
import org.apache.hms.common.entity.command.CreateCommand;
import org.apache.hms.common.entity.command.DeleteCommand;
import org.apache.hms.common.entity.command.StatusCommand;
import org.apache.hms.common.entity.manifest.ClusterHistory;
import org.apache.hms.common.entity.manifest.ClusterManifest;
import org.apache.hms.common.entity.Response;
import org.apache.hms.common.util.ExceptionUtil;
import org.apache.hms.common.util.JAXBUtil;
import org.apache.hms.common.util.ZookeeperUtil;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;

public class ClientHandler {
  private static Log LOG = LogFactory.getLog(ClientHandler.class);
  
  private ZooKeeper zk;
  
  public ClientHandler(ZooKeeper zk) {
    this.zk = zk;
    try {
      if (zk.exists(CommonConfigurationKeys.ZOOKEEPER_COMMAND_QUEUE_PATH_DEFAULT, null) == null) {
        LOG.error("HMS command queue at " + CommonConfigurationKeys.ZOOKEEPER_COMMAND_QUEUE_PATH_DEFAULT + " doesn't exist");
      }
    } catch (Exception e) {
      LOG.error(ExceptionUtil.getStackTrace(e));
    }
    LOG.info("Created one ClientHandler object");
  }
  
  public String queueCmd(Command cmd) throws KeeperException, InterruptedException, IOException {
    String path = zk.create(CommonConfigurationKeys.ZOOKEEPER_COMMAND_QUEUE_PATH_DEFAULT + "/cmd-", JAXBUtil.write(cmd), Ids.OPEN_ACL_UNSAFE,
        CreateMode.PERSISTENT_SEQUENTIAL);
    LOG.info("Queued command: " + cmd);
    return path.substring(path.lastIndexOf('/') + 1);
  }
  
//  public Response createCluster2(CreateClusterCommand cmd) throws IOException {
//    LOG.info("Received COMMAND: " + cmd);
//    String output = null;
//    Response r = new Response();
//    try {
//      ((ClusterManifest) cmd.getClusterManifest()).load();
//      String clusterName = cmd.getClusterManifest().getClusterName();
//      String clusterPath = CommonConfigurationKeys.ZOOKEEPER_CLUSTER_ROOT_DEFAULT + "/" + clusterName;
//      if (zk.exists(clusterPath, null) != null) {
//        String msg = "Cluster [" + clusterName + "] already exists. CREATE operation aborted.";
//        LOG.warn(msg);
//        r.setOutput(msg);
//        r.setCode(1);
//        return r;
//      }
//      output = queueCmd(cmd);
//    } catch (Exception e) {
//      LOG.warn(ExceptionUtil.getStackTrace(e));
//      r.setOutput(e.getMessage());
//      r.setCode(1);
//      return r;
//    }
//    r.setOutput(output);
//    r.setCode(0);
//    return r;
//  }

//  public Response createCluster(CreateCommand cmd) throws IOException {
//    LOG.info("Received COMMAND: " + cmd);
//    String output = null;
//    Response r = new Response();
//    try {
//      String clusterPath = CommonConfigurationKeys.ZOOKEEPER_CLUSTER_ROOT_DEFAULT + "/" + cmd.getClusterName();
//      if (zk.exists(clusterPath, null) != null) {
//        String msg = "Cluster [" + cmd.getClusterName() + "] already exists. CREATE operation aborted.";
//        LOG.warn(msg);
//        r.setOutput(msg);
//        r.setCode(1);
//        return r;
//      }
//      output = queueCmd(cmd);
//    } catch (Exception e) {
//      LOG.warn(ExceptionUtil.getStackTrace(e));
//      r.setOutput(e.getMessage());
//      r.setCode(1);
//      return r;
//    }
//    r.setOutput(output);
//    r.setCode(0);
//    return r;
//  }
//
//  public Response deleteCluster(DeleteCommand cmd) throws IOException {
//    LOG.info("Received COMMAND: " + cmd);
//    String output = null;
//    Response r = new Response();
//    try {
//      String clusterPath = CommonConfigurationKeys.ZOOKEEPER_CLUSTER_ROOT_DEFAULT + "/" + cmd.getClusterName();
//      if ( zk.exists(clusterPath, null) == null) {
//        String msg = "Cluster [" + cmd.getClusterName() + "] doesn't exist. Delete operation aborted.";
//        LOG.warn(msg);
//        r.setOutput(msg);
//        r.setCode(1);
//        return r;
//      }
//      output = queueCmd(cmd);
//    } catch (Exception e) {
//      LOG.warn(ExceptionUtil.getStackTrace(e));
//      r.setOutput(e.getMessage());
//      r.setCode(1);
//      return r;
//    }
//    r.setOutput(output);
//    r.setCode(0);
//    return r;
//  }
  
  public ClusterManifest checkClusterStatus(String clusterId) throws IOException {
    String clusterPath = ZookeeperUtil.getClusterPath(clusterId);
    try {
      if(zk.exists(clusterPath, null) == null) {
        throw new IOException("Cluster "+clusterId+" does not exist.");
      }
      ClusterHistory history = JAXBUtil.read(zk.getData(clusterPath, false, null), ClusterHistory.class);
      int index = history.getHistory().size()-1;
      ClusterManifest cm = history.getHistory().get(index);
      return cm;
    } catch(Throwable e) {
      throw new IOException(e);
    }
  }
  
//  public Response checkStatus(StatusCommand cmd) throws IOException {
//    LOG.info("Received COMMAND: " + cmd);
//    Response r = new Response();
//    try {
//      String nodePath = cmd.getNodePath();
//      if (nodePath != null) {
//        if (zk.exists(nodePath, null) == null) {
//          String msg = "Node " + nodePath + " doesn't exist";
//          LOG.warn(msg);
//          r.setOutput(msg);
//          r.setCode(1);
//          return r;
//        }
//        MachineState state = JAXBUtil.read(zk.getData(nodePath, false, null),
//            MachineState.class);
//        r.setOutput(state.toString());
//        r.setCode(0);
//        return r;
//      }
//      String cmdPath = CommonConfigurationKeys.ZOOKEEPER_COMMAND_QUEUE_PATH_DEFAULT + "/" + cmd.getCmdId();
//      if ( zk.exists(cmdPath, null) == null) {
//        String msg = "Command " + cmd.getCmdId() + " doesn't exist";
//        LOG.warn(msg);
//        r.setOutput(msg);
//        r.setCode(1);
//        return r;
//      }
//      String cmdStatusPath = cmdPath + CommandHandler.COMMAND_STATUS;
//      CommandStatus status = null;
//      try {
//        status = JAXBUtil.read(zk.getData(cmdStatusPath, false, null), CommandStatus.class);
//      } catch (KeeperException.NoNodeException e) {
//        r.setOutput("Command " + cmd.getCmdId() + ": not yet started");
//        r.setCode(0);
//        return r;
//      }
//      StringBuilder sb = new StringBuilder(status.toString());
//      List<String> children = zk.getChildren(cmdStatusPath, null);
//      if (children != null) {
//        for (String child : children) {
//          ActionStatus as = JAXBUtil.read(zk.getData(cmdStatusPath + "/" + child, false, null), ActionStatus.class);
//          sb.append("\nactionId=");
//          sb.append(as.getActionId());
//          sb.append(", host=");
//          sb.append(as.getHost());
//          sb.append(", status=");
//          sb.append(as.getStatus());
//          sb.append(", error msg: ");
//          sb.append(as.getError());
//        }
//      }
//      r.setOutput(sb.toString());
//      r.setCode(0);
//      return r;
//    } catch (Exception e) {
//      LOG.warn(ExceptionUtil.getStackTrace(e));
//      r.setOutput(e.getMessage());
//      r.setCode(1);
//      return r;
//    }
//  }
  
  public MachineState checkNodeStatus(String nodePath) throws IOException {
    LOG.info("Received Node Path: " + nodePath);
    try {
      if (zk.exists(nodePath, null) == null) {
        String msg = "Node " + nodePath + " doesn't exist";
        LOG.warn(msg);
        throw new IOException(msg);
      }
      MachineState state = JAXBUtil.read(zk.getData(nodePath, false, null),
          MachineState.class);
      return state;
    } catch (Exception e) {
      LOG.warn(ExceptionUtil.getStackTrace(e));
      throw new IOException(e);
    }
  }
  
  public CommandStatus checkCommandStatus(StatusCommand cmd) throws IOException {
    try {
      String cmdPath = CommonConfigurationKeys.ZOOKEEPER_COMMAND_QUEUE_PATH_DEFAULT + "/" + cmd.getCmdId();
      if ( zk.exists(cmdPath, null) == null) {
        String msg = "Command " + cmd.getCmdId() + " doesn't exist";
        LOG.warn(msg);
        throw new IOException(msg);
      }
      String cmdStatusPath = cmdPath + CommandHandler.COMMAND_STATUS;
      CommandStatus status = null;
      try {
        status = JAXBUtil.read(zk.getData(cmdStatusPath, false, null),
            CommandStatus.class);
      } catch (KeeperException.NoNodeException e) {
        String msg = "Command " + cmd.getCmdId() + ": not yet started";
        throw new IOException(msg);
      }
      return status;
    } catch (Exception e) {
      LOG.warn(ExceptionUtil.getStackTrace(e));
      throw new IOException(e);
    }
  }

  public List<Command> listCommand() throws IOException {
    List<Command> list = new ArrayList<Command>();
    try {
      String cmdPath = CommonConfigurationKeys.ZOOKEEPER_COMMAND_QUEUE_PATH_DEFAULT;
      if(zk.exists(cmdPath, null) == null) {
        throw new IOException("Command Queue does not exist.");
      }
      List<String> commands = zk.getChildren(cmdPath, null);
      for(String command : commands) {
        StringBuilder cmdStatusPath = new StringBuilder();
        cmdStatusPath.append(cmdPath);
        cmdStatusPath.append("/");
        cmdStatusPath.append(command);
        Command cmd = JAXBUtil.read(zk.getData(cmdStatusPath.toString(), false, null),
          Command.class);
        cmd.setId(command);
        list.add(cmd);
      }
      return list;
    } catch(Exception e) {
      LOG.warn(ExceptionUtil.getStackTrace(e));
      throw new IOException(e);      
    }
  }

  public List<ClusterManifest> listClusters() throws IOException {
    List<ClusterManifest> list = new ArrayList<ClusterManifest>();
    try {
      String cmdPath = CommonConfigurationKeys.ZOOKEEPER_CLUSTER_ROOT_DEFAULT;
      if(zk.exists(cmdPath, null) == null) {
        throw new IOException("No cluster exists.");
      }
      List<String> commands = zk.getChildren(cmdPath, null);
      for(String command : commands) {
        StringBuilder cmdStatusPath = new StringBuilder();
        cmdStatusPath.append(cmdPath);
        cmdStatusPath.append("/");
        cmdStatusPath.append(command);
        try {
          ClusterHistory history = JAXBUtil.read(zk.getData(cmdStatusPath.toString(), false, null),
            ClusterHistory.class);
          int index = history.getHistory().size()-1;
          ClusterManifest cluster = history.getHistory().get(index);
          list.add(cluster);
        } catch(EOFException skip) {
          // Skip cluster if the cluster node is in the process of being created.
        }
      }
      return list;
    } catch(Exception e) {
      LOG.warn(ExceptionUtil.getStackTrace(e));
      throw new IOException(e);      
    }
  }
}
