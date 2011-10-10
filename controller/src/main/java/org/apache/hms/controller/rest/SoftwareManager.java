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
import org.apache.hms.common.entity.manifest.NodesManifest;
import org.apache.hms.common.entity.manifest.PackageInfo;
import org.apache.hms.common.entity.manifest.Role;
import org.apache.hms.common.entity.manifest.SoftwareManifest;
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

@Path("software")
public class SoftwareManager {
  private static Log LOG = LogFactory.getLog(SoftwareManager.class);
  
  @GET
  @Path("stack/sample")
  public SoftwareManifest getSample() {
    SoftwareManifest sm = new SoftwareManifest();
    sm.setName("hadoop");
    sm.setVersion("0.23");
    PackageInfo[] packages = new PackageInfo[1];
    packages[0]= new PackageInfo();
    packages[0].setName("http://hrt8n36.cc1.ygridcore.net/hadoop-0.23.torrent");

    List<Role> roles = new ArrayList<Role>();
    Role role = new Role();
    role.setName("namenode");
    role.setPackages(packages);
    roles.add(role);
    
    role = new Role();
    role.setName("datanode");
    role.setPackages(packages);
    roles.add(role);
    
    role = new Role();
    role.setName("jobtracker");
    role.setPackages(packages);
    roles.add(role);
    
    role = new Role();
    role.setName("tasktracker");
    role.setPackages(packages);
    roles.add(role);

    sm.setRoles(roles);
    return sm;
  }
  
  @GET
  @Path("stack")
  public List<SoftwareManifest> getList() {
    List<SoftwareManifest> list = new ArrayList<SoftwareManifest>();
    try {
      ZooKeeper zk = Controller.getInstance().getZKInstance();
      List<String> stacks = zk.getChildren(CommonConfigurationKeys.ZOOKEEPER_SOFTWARE_MANIFEST_PATH_DEFAULT, false);
      Stat current = new Stat();
      for(String stack : stacks) {
        byte[] data = zk.getData(ZookeeperUtil.getSoftwareManifestPath(stack), false, current);
        SoftwareManifest x = JAXBUtil.read(data, SoftwareManifest.class);
        list.add(x);
      }
    } catch(Exception e) {
      LOG.error(ExceptionUtil.getStackTrace(e));
      throw new WebApplicationException(500);
    }
    return list;
  }
  
  @GET
  @Path("stack/{id}")
  public SoftwareManifest getSoftwareStack(@PathParam("id") String id) {
    try {
      ZooKeeper zk = Controller.getInstance().getZKInstance();
      Stat current = new Stat();
      String path = ZookeeperUtil.getSoftwareManifestPath(id);
      byte[] data = zk.getData(path, false, current);
      SoftwareManifest result = JAXBUtil.read(data, SoftwareManifest.class);
      return result;
    } catch(KeeperException.NoNodeException e) {
      throw new WebApplicationException(404);
    } catch(Exception e) {
      LOG.error(ExceptionUtil.getStackTrace(e));
      throw new WebApplicationException(500);
    }
  }
  
  @POST
  @Path("stack")
  public Response createStack(@Context UriInfo uri, SoftwareManifest stack) {
    ZooKeeper zk = Controller.getInstance().getZKInstance();
    String[] parts = stack.getUrl().toString().split("/");
    String label = ZookeeperUtil.getSoftwareManifestPath(parts[parts.length -1]);
    try {
      byte[] data = JAXBUtil.write(stack);      
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
  @Path("stack")
  public Response updateStack(SoftwareManifest stack) {
    ZooKeeper zk = Controller.getInstance().getZKInstance();
    try {
      byte[] data = JAXBUtil.write(stack);
      String id = ZookeeperUtil.getBaseURL(stack.getUrl().toString());
      Stat stat = zk.exists(CommonConfigurationKeys.ZOOKEEPER_SOFTWARE_MANIFEST_PATH_DEFAULT+'/'+id, false);
      zk.setData(CommonConfigurationKeys.ZOOKEEPER_SOFTWARE_MANIFEST_PATH_DEFAULT+'/'+id, data, stat.getVersion());
      Response r = new Response();
      r.setOutput("Update successful.");
      return r;
    } catch(Exception e) {
      LOG.error(ExceptionUtil.getStackTrace(e));
      throw new WebApplicationException(500);
    }
  }
  
  @DELETE
  @Path("stack/{id}")
  public Response deleteStack(@PathParam("id") String id) {
    ZooKeeper zk = Controller.getInstance().getZKInstance();
    try {
      String path = ZookeeperUtil.getSoftwareManifestPath(id);
      Stat current = zk.exists(path, false);
      zk.delete(path, current.getVersion());
    } catch(Exception e) {
      LOG.error(ExceptionUtil.getStackTrace(e));
      throw new WebApplicationException(500);      
    }
    Response r = new Response();
    r.setOutput("Software list: " + id + " deleted.");
    return r;
  }
}
