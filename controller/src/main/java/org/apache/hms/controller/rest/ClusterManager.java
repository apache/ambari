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
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hms.common.entity.cluster.MachineState;
import org.apache.hms.common.entity.command.CreateClusterCommand;
import org.apache.hms.common.entity.command.CreateCommand;
import org.apache.hms.common.entity.command.DeleteClusterCommand;
import org.apache.hms.common.entity.command.DeleteCommand;
import org.apache.hms.common.entity.command.StatusCommand;
import org.apache.hms.common.entity.manifest.ClusterManifest;
import org.apache.hms.common.entity.manifest.ConfigManifest;
import org.apache.hms.common.entity.manifest.NodesManifest;
import org.apache.hms.common.entity.manifest.SoftwareManifest;
import org.apache.hms.common.entity.Response;
import org.apache.hms.common.util.ExceptionUtil;
import org.apache.hms.controller.Controller;

@Path("cluster")
public class ClusterManager {
  private static String HOSTNAME;
  private static String DEFAULT_URL;
  
  public ClusterManager() {
    InetAddress addr;
    try {
      addr = InetAddress.getLocalHost();
      byte[] ipAddr = addr.getAddress();
      HOSTNAME = addr.getHostName();
    } catch (UnknownHostException e) {
      HOSTNAME = "localhost";
    }
    StringBuilder buffer = new StringBuilder();
    buffer.append("http://");
    buffer.append(HOSTNAME);
    buffer.append(":");
    buffer.append(Controller.CONTROLLER_PORT);
    buffer.append("/");
    buffer.append(Controller.CONTROLLER_PREFIX);
    DEFAULT_URL = buffer.toString();
  }
  
  private static Log LOG = LogFactory.getLog(ClusterManager.class);
  
//  @POST
//  @Path("create")
//  public Response createCluster(CreateCommand cmd) {
//    try {
//      return Controller.getInstance().getClientHandler().createCluster(cmd);
//    } catch (IOException e) {
//      throw new WebApplicationException(e);
//    }
//  }
//  
//  @POST
//  @Path("delete")
//  public Response deleteCluster(DeleteCommand cmd) {
//    try {
//      LOG.info("received: " + cmd);
//      Response r = Controller.getInstance().getClientHandler().deleteCluster(cmd);
//      LOG.info("response is: " + r.getOutput());
//      return r;
//    } catch (IOException e) {
//      LOG.warn("got excpetion: " + e);
//      throw new WebApplicationException(e);
//    }
//  }
  
  @GET
  @Path("status/{clusterId}")
  public ClusterManifest checkStatus(@PathParam("clusterId") String clusterId) {
    try {
      return Controller.getInstance().getClientHandler().checkClusterStatus(clusterId);
    } catch (IOException e) {
      LOG.warn(ExceptionUtil.getStackTrace(e));
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("node/status")
  public MachineState checkNodeStatus(@QueryParam("node") String nodeId) {
    try {
      return Controller.getInstance().getClientHandler().checkNodeStatus(nodeId);
    } catch (IOException e) {
      LOG.warn(ExceptionUtil.getStackTrace(e));
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("manifest/create-cluster-sample")
  public CreateClusterCommand getCreateClusterSample(@QueryParam("expand") boolean expand, @QueryParam("name") String clusterName) {
    try {
      URL nodeUrl = new URL(DEFAULT_URL+"/nodes/manifest/sample");
      URL softwareUrl = new URL(DEFAULT_URL+"/software/manifest/sample");
      URL configUrl = new URL(DEFAULT_URL+"/config/manifest/create-hadoop-cluster");
      CreateClusterCommand command = new CreateClusterCommand();
      ClusterManifest cm = new ClusterManifest();
      if(clusterName!=null) {
        cm.setClusterName(clusterName);
      }
      NodesManifest nodesM = new NodesManifest();
      nodesM.setUrl(nodeUrl);
      cm.setNodes(nodesM);
      SoftwareManifest softwareM = new SoftwareManifest();
      softwareM.setUrl(softwareUrl);
      cm.setSoftware(softwareM);
      ConfigManifest configM = new ConfigManifest();
      configM.setUrl(configUrl);
      cm.setConfig(configM);
      if (expand) {
        cm.load();
      }
      command.setClusterManifest(cm);
      return command;
    } catch (IOException e) {
      throw new WebApplicationException(e);
    }
  }
  
  @GET
  @Path("manifest/delete-cluster-sample")
  public DeleteClusterCommand getDeleteClusterSample(@QueryParam("expand") boolean expand, @QueryParam("name") String clusterName) {
    try {
      URL nodeUrl = new URL(DEFAULT_URL+"/nodes/manifest/sample");
      URL softwareUrl = new URL(DEFAULT_URL+"/software/manifest/sample");
      URL configUrl = new URL(DEFAULT_URL+"/config/manifest/delete-hadoop-cluster");

      DeleteClusterCommand command = new DeleteClusterCommand();
      ClusterManifest cm = new ClusterManifest();
      if(clusterName!=null) {
        cm.setClusterName(clusterName);
      }
      NodesManifest nodesM = new NodesManifest();
      nodesM.setUrl(nodeUrl);
      cm.setNodes(nodesM);
      SoftwareManifest softwareM = new SoftwareManifest();
      softwareM.setUrl(softwareUrl);
      cm.setSoftware(softwareM);
      ConfigManifest configM = new ConfigManifest();
      configM.setUrl(configUrl);
      cm.setConfig(configM);
      if (expand) {
        cm.load();
      }
      command.setClusterManifest(cm);
      return command;
    } catch (IOException e) {
      throw new WebApplicationException(e);
    }
  }
  
  @GET
  @Path("list")
  public List<ClusterManifest> listClusters() {
    try {
      List<ClusterManifest> list = Controller.getInstance().getClientHandler().listClusters();
      return list;
    } catch(IOException e) {
      throw new WebApplicationException(e);
    }
  }
}
