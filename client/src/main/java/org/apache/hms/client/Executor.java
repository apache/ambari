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

package org.apache.hms.client;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hms.common.entity.cluster.MachineState;
import org.apache.hms.common.entity.command.Command;
import org.apache.hms.common.entity.command.CommandStatus;
import org.apache.hms.common.entity.command.Command.CmdType;
import org.apache.hms.common.entity.manifest.ClusterManifest;
import org.apache.hms.common.entity.Response;
import org.apache.hms.common.util.ExceptionUtil;
import org.apache.hms.common.util.JAXBUtil;


import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

public class Executor {
  private static Log LOG = LogFactory.getLog(Executor.class);

  private static Executor instance;
  private static String CONTROLLER = "localhost:4080/v1";
  
  public Executor() {
  }
  
  /**
   * Executor manages the Rest API communication between HMS command line client and HMS Controller 
   * @return
   */
  public static Executor getInstance() {
    if(instance == null) {
      instance = new Executor();
    }
    return instance;
  }
  
  /**
   * Generic method to call HMS Controller Rest API for issuing commands.
   * @param cmd - Command Object
   * @return
   * @throws IOException
   */
  public Response sendToController(Command cmd) throws IOException {
    try {
      StringBuilder url = new StringBuilder();
      url.append("http://");
      url.append(CONTROLLER);
      url.append("/controller");
      Client wsClient = Client.create();
      WebResource webResource = wsClient.resource(url.toString());
      Response result;
      if(cmd instanceof org.apache.hms.common.entity.command.CreateClusterCommand) {
        result = webResource.path("create/cluster").type(MediaType.APPLICATION_JSON_TYPE).post(Response.class, cmd);        
      } else if(cmd instanceof org.apache.hms.common.entity.command.UpgradeClusterCommand) {
        result = webResource.path("upgrade/cluster").type(MediaType.APPLICATION_JSON_TYPE).post(Response.class, cmd);        
      } else if(cmd instanceof org.apache.hms.common.entity.command.DeleteClusterCommand) {
        result = webResource.path("delete/cluster").type(MediaType.APPLICATION_JSON_TYPE).post(Response.class, cmd); 
      } else if (cmd instanceof org.apache.hms.common.entity.command.DeleteCommand) {
        webResource.path("delete/command").path(cmd.getId()).type(MediaType.APPLICATION_JSON_TYPE).delete(); 
        result = new Response();
        result.setCode(0);
        result.setOutput(cmd.getId()+" command deleted.");
      } else {
        result = webResource.type("application/json").get(Response.class);         
      }
      return result;
    } catch(Exception e) {
      LOG.error(ExceptionUtil.getStackTrace(e));
      throw new IOException(e);
    }
  }
  
  /**
   * Call HMS Controller Rest API to query command status.
   * @param id - Command ID
   * @return
   * @throws IOException
   * @throws WebApplicationException
   */
  public CommandStatus queryController(String id) throws IOException, WebApplicationException {
    StringBuilder url = new StringBuilder();
    url.append("http://");
    url.append(CONTROLLER);
    url.append("/command/status/");
    url.append(id);
    Client wsClient = Client.create();
    WebResource webResource = wsClient.resource(url.toString());
    CommandStatus result = webResource.type("application/json").get(CommandStatus.class);         
    return result;
  }

  /**
   * Call HMS Controller Rest API to query cluster status.
   * @param clusterId - Cluster ID
   * @return
   * @throws IOException
   */
  public ClusterManifest checkClusterStatus(String clusterId) throws IOException {
    try {
      StringBuilder url = new StringBuilder();
      url.append("http://");
      url.append(CONTROLLER);
      url.append("/cluster/status/");
      url.append(clusterId);
      Client wsClient = Client.create();
      WebResource webResource = wsClient.resource(url.toString());
      ClusterManifest result = webResource.type("application/json").get(ClusterManifest.class);         
      return result;
    } catch(Exception e) {
      LOG.error(ExceptionUtil.getStackTrace(e));
      throw new IOException(e);
    }    
  }
  
  /**
   * Call HMS Controller Rest API to query node status.
   * @param nodeId - Full path to the node in ZooKeeper
   * @return
   * @throws IOException
   */
  public MachineState checkNodeStatus(String nodeId) throws IOException {
    try {
      StringBuilder url = new StringBuilder();
      url.append("http://");
      url.append(CONTROLLER);
      url.append("/cluster/node/status");
      Client wsClient = Client.create();
      WebResource webResource = wsClient.resource(url.toString()).queryParam("node", nodeId);
      MachineState result = webResource.type("application/json").get(MachineState.class);         
      return result;
    } catch(Exception e) {
      LOG.error(ExceptionUtil.getStackTrace(e));
      throw new IOException(e);
    }    
  }

}
