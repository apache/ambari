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

package org.apache.hms.controller.rest;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hms.common.conf.CommonConfigurationKeys;
import org.apache.hms.common.entity.Response;
import org.apache.hms.common.entity.command.Command;
import org.apache.hms.common.entity.command.CommandStatus;
import org.apache.hms.common.entity.command.CreateClusterCommand;
import org.apache.hms.common.entity.command.DeleteClusterCommand;
import org.apache.hms.common.entity.command.StatusCommand;
import org.apache.hms.common.entity.command.UpgradeClusterCommand;
import org.apache.hms.common.util.ExceptionUtil;
import org.apache.hms.controller.ClientHandler;
import org.apache.hms.controller.CommandHandler;
import org.apache.hms.controller.Controller;

@Path("controller")
public class ControllerManager {
  private static Log LOG = LogFactory.getLog(ControllerManager.class);

  @GET
  @Path("command/status/{command}")
  public CommandStatus checkCommandStatus(@PathParam("command") String cmdId) {
    StatusCommand cmd = new StatusCommand();
    cmd.setCmdId(cmdId);
    try {
      return Controller.getInstance().getClientHandler().checkCommandStatus(cmd);
    } catch (IOException e) {
      throw new WebApplicationException(404);
    }
  }

//  @GET
//  @Path("cluster/status/{clusterName}")
//  public Response checkClusterStatus(@PathParam("clusterName") String cmdId) {
//    StatusCommand cmd = new StatusCommand();
//    cmd.setCmdId(cmdId);
//    try {
//      return Controller.getInstance().getClientHandler().checkStatus(cmd);
//    } catch (IOException e) {
//      throw new WebApplicationException(e);
//    }
//  }
  
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("create/cluster")
  public Response createCluster(CreateClusterCommand cmd) {
    try {
      Controller ci = Controller.getInstance();
      ClientHandler ch = ci.getClientHandler();
      if(ch==null) {
        LOG.error("ClientHandler is empty");
      }
      String path=ch.queueCmd(cmd);
      Response r = new Response();
      r.setOutput(path);
      return r;
    } catch (Exception e) {
      LOG.error(ExceptionUtil.getStackTrace(e));
      throw new WebApplicationException(e);
    }
  }
  
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("delete/cluster")
  public Response deleteCluster(DeleteClusterCommand cmd) {
    try {
      Controller ci = Controller.getInstance();
      ClientHandler ch = ci.getClientHandler();
      if(ch==null) {
        LOG.error("ClientHandler is empty");
      }
      String path=ch.queueCmd(cmd);
      Response r = new Response();
      r.setOutput(path);
      return r;
    } catch (Exception e) {
      LOG.error(ExceptionUtil.getStackTrace(e));
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("upgrade/cluster")
  public Response upgradeCluster(UpgradeClusterCommand cmd) {
    try {
      Controller ci = Controller.getInstance();
      ClientHandler ch = ci.getClientHandler();
      if(ch==null) {
        LOG.error("ClientHandler is empty");
      }
      String path=ch.queueCmd(cmd);
      Response r = new Response();
      r.setOutput(path);
      return r;
    } catch (Exception e) {
      LOG.error(ExceptionUtil.getStackTrace(e));
      throw new WebApplicationException(e);
    }
  }
  
  @DELETE
  @Path("abort/{command}")
  public Response abortCommand(@PathParam("command") String cmdId) {
    try {
      Controller ci = Controller.getInstance();
      CommandHandler ch = ci.getCommandHandler();
      if(ch==null) {
        LOG.error("ClientHandler is empty");
      }
      String cmdPath = CommonConfigurationKeys.ZOOKEEPER_COMMAND_QUEUE_PATH_DEFAULT + "/" + cmdId;
      Command cmd = ch.getCommand(cmdPath);
      ch.failCommand(cmdPath, cmd);
      Response r = new Response();
      r.setOutput(cmdId + " is aborted.");
      return r;
    } catch(Exception e) {
      LOG.error(ExceptionUtil.getStackTrace(e));
      throw new WebApplicationException(e);      
    }
  }
  
  @DELETE
  @Path("delete/{command}")
  public Response deleteCommand(@PathParam("command") String cmdId) {
    return null;    
  }
}
