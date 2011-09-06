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

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hms.common.conf.CommonConfigurationKeys;
import org.apache.hms.common.entity.Response;
import org.apache.hms.common.entity.manifest.Node;
import org.apache.hms.common.entity.manifest.NodesManifest;
import org.apache.hms.common.entity.manifest.Role;
import org.apache.hms.common.util.ExceptionUtil;
import org.apache.hms.common.util.HostUtil;
import org.apache.hms.common.util.JAXBUtil;
import org.apache.hms.common.util.ZookeeperUtil;
import org.apache.hms.controller.Controller;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;

@Path("nodes")
public class NodesManager {
  private static Log LOG = LogFactory.getLog(NodesManager.class);
  
  @GET
  @Path("manifest/sample")
  public NodesManifest getSample() {
    String[] hosts = { "localhost" };
    NodesManifest n = new NodesManifest();
    List<Role> roles = new ArrayList<Role>();
    Role role = new Role();
    role.setName("namenode");
    String[] namenode = { "hrt8n37.cc1.ygridcore.net" };
    role.setHosts(namenode);
    roles.add(role);
    role = new Role();
    role.setName("datanode");
    String[] datanode = { "hrt8n38.cc1.ygridcore.net", "hrt8n39.cc1.ygridcore.net" };
    role.setHosts(datanode);
    roles.add(role);
    role = new Role();
    role.setName("jobtracker");
    String[] jobtracker = { "hrt8n37.cc1.ygridcore.net" };
    role.setHosts(jobtracker);
    roles.add(role);
    role = new Role();
    role.setName("tasktracker");
    String[] tasktracker = { "hrt8n38.cc1.ygridcore.net", "hrt8n39.cc1.ygridcore.net" };
    role.setHosts(tasktracker);
    roles.add(role);
    n.setNodes(roles);
    return n;
  }
  
  @GET
  @Path("manifest")
  public List<NodesManifest> getList() {
    List<NodesManifest> list = new ArrayList<NodesManifest>();
    try {
      ZooKeeper zk = Controller.getInstance().getZKInstance();
      List<String> nodes = zk.getChildren(CommonConfigurationKeys.ZOOKEEPER_NODES_MANIFEST_PATH_DEFAULT, false);
      Stat current = new Stat();
      for(String nodeList : nodes) {
        byte[] data = zk.getData(ZookeeperUtil.getNodesManifestPath(nodeList), false, current);
        NodesManifest x = JAXBUtil.read(data, NodesManifest.class);
        list.add(x);
      }
    } catch(Exception e) {
      LOG.error(ExceptionUtil.getStackTrace(e));
      throw new WebApplicationException(500);
    }
    return list;
  }
  
  @GET
  @Path("manifest/{id}")
  public NodesManifest get(@PathParam("id") String id) {
    try {
      ZooKeeper zk = Controller.getInstance().getZKInstance();
      Stat current = new Stat();
      String path = ZookeeperUtil.getNodesManifestPath(id);
      byte[] data = zk.getData(path, false, current);
      NodesManifest result = JAXBUtil.read(data, NodesManifest.class);
      return result;
    } catch(KeeperException.NoNodeException e) {
      throw new WebApplicationException(404);
    } catch(Exception e) {
      LOG.error(ExceptionUtil.getStackTrace(e));
      throw new WebApplicationException(500);
    }
  }
  
  @POST
  @Path("manifest")
  public Response createManifest(@Context UriInfo uri, NodesManifest newManifest) {
    ZooKeeper zk = Controller.getInstance().getZKInstance();
    List<Role> roles = newManifest.getRoles();
    List<Role> testedRoles = new ArrayList<Role>();
    for(Role role : roles) {
      String[] hosts = role.getHosts();
      ArrayList<String> list = new ArrayList<String>(); 
      for(String host : hosts) {
        String[] parts = host.split(",");
        HostUtil util = new HostUtil(parts);
        list.addAll(util.generate());
      }
      role.setHosts(list.toArray(new String[list.size()]));
      testedRoles.add(role);
    }
    newManifest.setNodes(testedRoles);
    String[] parts = newManifest.getUrl().toString().split("/");
    String label = ZookeeperUtil.getNodesManifestPath(parts[parts.length -1]);
    try {
      byte[] data = JAXBUtil.write(newManifest);      
      String id = zk.create(label, data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
      Response r = new Response();
      r.setOutput(id);
      return r;
    } catch(Exception e) {
      LOG.error(ExceptionUtil.getStackTrace(e));
      throw new WebApplicationException(500);
    }
  }
  
  @PUT
  @Path("manifest")
  public Response updateManifest(NodesManifest updates) {
    ZooKeeper zk = Controller.getInstance().getZKInstance();
    try {
      byte[] data = JAXBUtil.write(updates);
      String id = ZookeeperUtil.getBaseURL(updates.getUrl().toString());
      Stat stat = zk.exists(CommonConfigurationKeys.ZOOKEEPER_NODES_MANIFEST_PATH_DEFAULT+'/'+id, false);
      zk.setData(CommonConfigurationKeys.ZOOKEEPER_NODES_MANIFEST_PATH_DEFAULT+'/'+id, data, stat.getVersion());
      Response r = new Response();
      r.setOutput("Update successful.");
      return r;
    } catch(Exception e) {
      LOG.error(ExceptionUtil.getStackTrace(e));
      throw new WebApplicationException(500);
    }
  }
  
  @DELETE
  @Path("manifest/{id}")
  public Response deleteManifest(@PathParam("id") String id) {
    ZooKeeper zk = Controller.getInstance().getZKInstance();
    try {
      String path = ZookeeperUtil.getNodesManifestPath(id);
      Stat current = zk.exists(path, false);
      zk.delete(path, current.getVersion());
    } catch(Exception e) {
      LOG.error(ExceptionUtil.getStackTrace(e));
      throw new WebApplicationException(500);      
    }
    Response r = new Response();
    r.setOutput("Node list: " + id + " deleted.");
    return r;
  }
}
