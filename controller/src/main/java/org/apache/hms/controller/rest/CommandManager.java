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
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hms.common.entity.command.Command;
import org.apache.hms.common.entity.command.CommandStatus;
import org.apache.hms.common.entity.command.StatusCommand;
import org.apache.hms.common.util.ExceptionUtil;
import org.apache.hms.controller.Controller;

@Path("command")
public class CommandManager {
  private static Log LOG = LogFactory.getLog(CommandManager.class);
  
  @GET
  @Path("status/{command}")
  public CommandStatus checkStatus(@PathParam("command") String cmdId) {
    StatusCommand cmd = new StatusCommand();
    cmd.setCmdId(cmdId);
    try {
      CommandStatus status = Controller.getInstance().getClientHandler().checkCommandStatus(cmd);
      return status;
    } catch (IOException e) {
      LOG.error(e.getMessage());
      throw new WebApplicationException(404);
    }
  }
  
  @GET
  @Path("list")
  public List<Command> list() {
    try {
      List<Command> list = Controller.getInstance().getClientHandler().listCommand();
      return list;
    } catch (IOException e) {
      LOG.error(ExceptionUtil.getStackTrace(e));
      throw new WebApplicationException(500);
    }
  }
}
