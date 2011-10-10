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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hms.common.conf.CommonConfigurationKeys;
import org.apache.hms.common.entity.Status;
import org.apache.hms.common.entity.action.Action;
import org.apache.hms.common.entity.action.ActionDependency;
import org.apache.hms.common.entity.action.DaemonAction;
import org.apache.hms.common.entity.action.PackageAction;
import org.apache.hms.common.entity.action.ScriptAction;
import org.apache.hms.common.entity.cluster.MachineState.StateEntry;
import org.apache.hms.common.entity.cluster.MachineState.StateType;
import org.apache.hms.common.entity.manifest.ConfigManifest;
import org.apache.hms.common.util.ExceptionUtil;
import org.apache.hms.common.util.JAXBUtil;
import org.apache.hms.common.util.ZookeeperUtil;
import org.apache.hms.controller.Controller;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;


@Path("config")
public class ConfigManager {
  private static Log LOG = LogFactory.getLog(ConfigManager.class);
  
  @GET
  @Path("manifest/create-hadoop-cluster")
  public ConfigManifest getSample(@Context UriInfo uri) {
    List<Action> actions = new ArrayList<Action>();

    // Install software
    PackageAction install = new PackageAction();
    install.setActionType("install");
    //install.setRole("namenode");
    actions.add(install);
    List<StateEntry> expectedInstallResults = new LinkedList<StateEntry>();
    expectedInstallResults.add(new StateEntry(StateType.PACKAGE, "hadoop", Status.INSTALLED));
    install.setExpectedResults(expectedInstallResults);
    
    // Install software
    //install = new PackageAction();
    //install.setActionType("install");
    //install.setRole("datanode");
    //actions.add(install);

    // Install software
    //install = new PackageAction();
    //install.setActionType("install");
    //install.setRole("jobtracker");
    //actions.add(install);

    // Install software
    //install = new PackageAction();
    //install.setActionType("install");
    //install.setRole("tasktracker");
    //actions.add(install);

    // Generate Hadoop configuration
    ScriptAction setupConfig = new ScriptAction();
    setupConfig.setScript("/usr/sbin/hadoop-setup-conf.sh");
    int i=0;
    String[] parameters = new String[9];
    parameters[i++] = "--namenode-url=hdfs://${namenode}:9000/";
    parameters[i++] = "--jobtracker-url=${jobtracker}:9001";
    parameters[i++] = "--conf-dir=/etc/hadoop";
    parameters[i++] = "--hdfs-dir=/grid/0/hadoop/var/hdfs";
    parameters[i++] = "--namenode-dir=/grid/0/hadoop/var/hdfs/namenode";
    parameters[i++] = "--mapred-dir=/grid/0/tmp/mapred-local,/grid/1/tmp/mapred-local,/grid/2/tmp/mapred-local,/grid/3/tmp/mapred-local,/grid/4/tmp/mapred-local,/grid/5/tmp/mapred-local";
    parameters[i++] = "--datanode-dir=/grid/0/hadoop/var/hdfs/data,/grid/1/hadoop/var/hdfs/data,/grid/2/hadoop/var/hdfs/data,/grid/3/hadoop/var/hdfs/data,/grid/4/hadoop/var/hdfs/data,/grid/5/hadoop/var/hdfs/data";
    parameters[i++] = "--log-dir=/var/log/hadoop";
    parameters[i++] = "--auto";
    setupConfig.setParameters(parameters);
    List<StateEntry> expectedConfigResults = new LinkedList<StateEntry>();
    expectedConfigResults.add(new StateEntry(StateType.PACKAGE, "hadoop-config", Status.INSTALLED));
    setupConfig.setExpectedResults(expectedConfigResults);
    actions.add(setupConfig);
    
    // Format HDFS
    ScriptAction setupHdfs = new ScriptAction();
    setupHdfs.setRole("namenode");
    setupHdfs.setScript("/usr/sbin/hadoop-setup-hdfs.sh");
    String[] hdfsParameters = new String[2];
    hdfsParameters[0]="-c";
    hdfsParameters[1]="oxygen";
    setupHdfs.setParameters(hdfsParameters);
    // Setup dependencies       
    List<ActionDependency> dep = new LinkedList<ActionDependency>();
    Set<String> roles = new HashSet<String>();
    List<StateEntry> states = new LinkedList<StateEntry>();
    states.add(new StateEntry(StateType.PACKAGE, "hadoop-config", Status.INSTALLED));
    roles.add("namenode");
    roles.add("datanode");
    roles.add("jobtracker");
    roles.add("tasktracker");
    dep.add(new ActionDependency(roles, states));
    setupHdfs.setDependencies(dep);
    // Setup expected result
    List<StateEntry> expectedFormatResults = new LinkedList<StateEntry>();
    expectedFormatResults.add(new StateEntry(StateType.DAEMON, "hadoop-namenode", Status.STARTED));
    setupHdfs.setExpectedResults(expectedFormatResults);
    actions.add(setupHdfs);
    
    // Start Datanodes
    DaemonAction dataNodeAction = new DaemonAction();
    dataNodeAction.setDaemonName("hadoop-datanode");
    dataNodeAction.setActionType("start");
    dataNodeAction.setRole("datanode");
    // Setup namenode started dependencies
    dep = new LinkedList<ActionDependency>();
    roles = new HashSet<String>();
    states = new LinkedList<StateEntry>();
    states.add(new StateEntry(StateType.DAEMON, "hadoop-namenode", Status.STARTED));
    roles.add("namenode");
    dep.add(new ActionDependency(roles, states));
    dataNodeAction.setDependencies(dep);
    // Setup expected result
//    List<StateEntry> expectedDatanodeResults = new LinkedList<StateEntry>();
//    expectedDatanodeResults.add(new StateEntry(StateType.DAEMON, "hadoop-datanode", Status.STARTED));
//    dataNodeAction.setExpectedResults(expectedDatanodeResults);
    actions.add(dataNodeAction);

    // Start Jobtracker
    DaemonAction jobTrackerAction = new DaemonAction();
    jobTrackerAction.setDaemonName("hadoop-jobtracker");
    jobTrackerAction.setActionType("start");
    jobTrackerAction.setRole("jobtracker");
    // Setup datanode started dependencies
    dep = new LinkedList<ActionDependency>();
    roles = new HashSet<String>();
    states = new LinkedList<StateEntry>();
    states.add(new StateEntry(StateType.DAEMON, "hadoop-datanode", Status.STARTED));
    roles.add("datanode");
    dep.add(new ActionDependency(roles, states));
    jobTrackerAction.setDependencies(dep);
    // Setup expected result
//    List<StateEntry> expectedJobtrackerResults = new LinkedList<StateEntry>();
//    expectedJobtrackerResults.add(new StateEntry(StateType.DAEMON, "hadoop-jobtracker", Status.STARTED));
//    jobTrackerAction.setExpectedResults(expectedJobtrackerResults);
    actions.add(jobTrackerAction);
    
    // Start Tasktrackers
    DaemonAction taskTrackerAction = new DaemonAction();
    taskTrackerAction.setDaemonName("hadoop-tasktracker");
    taskTrackerAction.setActionType("start");
    taskTrackerAction.setRole("tasktracker");
    // Setup tasktracker started dependencies
    dep = new LinkedList<ActionDependency>();
    roles = new HashSet<String>();
    states = new LinkedList<StateEntry>();
    states.add(new StateEntry(StateType.DAEMON, "hadoop-jobtracker", Status.STARTED));
    roles.add("jobtracker");
    dep.add(new ActionDependency(roles, states));
    taskTrackerAction.setDependencies(dep);
    // Setup expected result
//    List<StateEntry> expectedTasktrackerResults = new LinkedList<StateEntry>();
//    expectedTasktrackerResults.add(new StateEntry(StateType.DAEMON, "hadoop-tasktracker", Status.STARTED));
//    taskTrackerAction.setExpectedResults(expectedTasktrackerResults);
    actions.add(taskTrackerAction);
    
    ConfigManifest cm = new ConfigManifest();
//    try {
//      cm.setUrl(uri.getAbsolutePath().toURL());
//    } catch (MalformedURLException e) {
//      throw new WebApplicationException(500);
//    }
    cm.setActions(actions);
    return cm;
  }
  

  @GET
  @Path("manifest/delete-cluster")
  public ConfigManifest getDestroyCluster() {
    List<Action> actions = new ArrayList<Action>();
    ScriptAction nuke = new ScriptAction();
    nuke.setScript("ps");
    int i=0;
    String[] parameters = new String[3];
    parameters[i++] = "ax | grep hms/apps | grep -v grep | cut -b 1-5 | xargs kill -9 || exit 0";
    nuke.setParameters(parameters);
    actions.add(nuke);

    ScriptAction nuke2 = new ScriptAction();
    nuke2.setScript("killall");
    i=0;
    String[] jsvcParameters = new String[3];
    jsvcParameters[i++] = "-9";
    jsvcParameters[i++] = "jsvc";
    jsvcParameters[i++] = "|| exit 0";
    nuke2.setParameters(jsvcParameters);
    nuke2.setRole("datanode");
    actions.add(nuke2);

    ScriptAction nukePackages = new ScriptAction();
    nukePackages.setScript("rpm");
    i=0;
    String[] packagesParameters = new String[8];
    packagesParameters[i++] = "-e";
    packagesParameters[i++] = "hadoop";
    packagesParameters[i++] = "||";
    packagesParameters[i++] = "rpm";
    packagesParameters[i++] = "-e";
    packagesParameters[i++] = "hadoop-mapreduce";
    packagesParameters[i++] = "hadoop-hdfs";
    packagesParameters[i++] = "hadoop-common || rm -rf /home/hms/apps/*";
    nukePackages.setParameters(packagesParameters);
    actions.add(nukePackages);
    ScriptAction scrub = new ScriptAction();
    scrub.setScript("rm");
    String[] scrubParameters = new String[2];
    scrubParameters[0] = "-rf";
    scrubParameters[1] = "/grid/[0-3]/hadoop/var";
    scrub.setParameters(scrubParameters);
    actions.add(scrub);
    
    ConfigManifest cm = new ConfigManifest();
    cm.setActions(actions);
    return cm;
  }

  @GET
  @Path("manifest/delete-hadoop-cluster")
  public ConfigManifest getDeleteCluster() {
    List<StateEntry> states = new LinkedList<StateEntry>();
    Set<String> roles = new HashSet<String>();
    List<Action> actions = new ArrayList<Action>();
    // Stop Tasktrackers
    DaemonAction taskTrackerAction = new DaemonAction();
    taskTrackerAction.setDaemonName("hadoop-tasktracker");
    taskTrackerAction.setActionType("stop");
    taskTrackerAction.setRole("tasktracker");
    // Setup expected result
    List<StateEntry> expectedTasktrackerResults = new LinkedList<StateEntry>();
    expectedTasktrackerResults.add(new StateEntry(StateType.DAEMON, "hadoop-tasktracker", Status.STOPPED));
    taskTrackerAction.setExpectedResults(expectedTasktrackerResults);
    actions.add(taskTrackerAction);

    // Stop Jobtracker
    DaemonAction jobTrackerAction = new DaemonAction();
    jobTrackerAction.setDaemonName("hadoop-jobtracker");
    jobTrackerAction.setActionType("stop");
    jobTrackerAction.setRole("jobtracker");
    // Setup tasktracker stop dependencies
    List<ActionDependency> dep = new LinkedList<ActionDependency>();
    roles = new HashSet<String>();
    states = new LinkedList<StateEntry>();
    states.add(new StateEntry(StateType.DAEMON, "hadoop-jobtracker", Status.STARTED));
    roles.add("jobtracker");
    dep.add(new ActionDependency(roles, states));
    jobTrackerAction.setDependencies(dep);
    // Setup expected result
    List<StateEntry> expectedJobtrackerResults = new LinkedList<StateEntry>();
    expectedJobtrackerResults.add(new StateEntry(StateType.DAEMON, "hadoop-jobtracker", Status.STOPPED));
    jobTrackerAction.setExpectedResults(expectedJobtrackerResults);
    actions.add(jobTrackerAction);

    // Stop Datanodes
    DaemonAction datanodeAction = new DaemonAction();
    datanodeAction.setDaemonName("hadoop-datanode");
    datanodeAction.setActionType("stop");
    datanodeAction.setRole("datanode");
    // Setup datanode stop dependencies
    dep = new LinkedList<ActionDependency>();
    roles = new HashSet<String>();
    states = new LinkedList<StateEntry>();
    states.add(new StateEntry(StateType.DAEMON, "hadoop-datanode", Status.STARTED));
    roles.add("datanode");
    dep.add(new ActionDependency(roles, states));
    datanodeAction.setDependencies(dep);
    // Setup expected result
    List<StateEntry> expectedDatanodeResults = new LinkedList<StateEntry>();
    expectedDatanodeResults.add(new StateEntry(StateType.DAEMON, "hadoop-datanode", Status.STOPPED));
    datanodeAction.setExpectedResults(expectedDatanodeResults);
    actions.add(datanodeAction);

    // Stop Namenode
    DaemonAction namenodeAction = new DaemonAction();
    namenodeAction.setDaemonName("hadoop-namenode");
    namenodeAction.setActionType("stop");
    namenodeAction.setRole("namenode");
    // Setup namenode stop dependencies
    dep = new LinkedList<ActionDependency>();
    roles = new HashSet<String>();
    states = new LinkedList<StateEntry>();
    states.add(new StateEntry(StateType.DAEMON, "hadoop-namenode", Status.STARTED));
    roles.add("namenode");
    dep.add(new ActionDependency(roles, states));
    namenodeAction.setDependencies(dep);
    // Setup expected result
    List<StateEntry> expectedNamenodeResults = new LinkedList<StateEntry>();
    expectedNamenodeResults.add(new StateEntry(StateType.DAEMON, "hadoop-namenode", Status.STOPPED));
    namenodeAction.setExpectedResults(expectedNamenodeResults);
    actions.add(namenodeAction);
    ConfigManifest cm = new ConfigManifest();
    cm.setActions(actions);
    return cm;
  }
  
  @POST
  @Path("blueprint/{name}")
  public Response createBlueprint(@Context UriInfo uri, 
      @PathParam("name") String name, ConfigManifest blueprint) {
    Response res;
    try {
      ZooKeeper zk = Controller.getInstance().getZKInstance();
      blueprint.setUrl(uri.getAbsolutePath().toURL());
      byte[] data = JAXBUtil.write(blueprint);
      Stat stat = zk.exists(CommonConfigurationKeys.ZOOKEEPER_CONFIG_BLUEPRINT_PATH_DEFAULT+'/'+name, false);
      if(stat==null) {
        zk.create(CommonConfigurationKeys.ZOOKEEPER_CONFIG_BLUEPRINT_PATH_DEFAULT+'/'+name, data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
      } else {
        throw new WebApplicationException(409);
      }
      res = Response.created(uri.getAbsolutePath()).build();
      return res;
    } catch(WebApplicationException e) {
      throw e;
    } catch(Exception e) {
      LOG.error(ExceptionUtil.getStackTrace(e));
      throw new WebApplicationException(500);
    }
  }
  
  @PUT
  @Path("blueprint/{name}")
  public Response updateBlueprint(@Context UriInfo uri, 
      @PathParam("name") String oldName, ConfigManifest blueprint) {
    Response res;
    try {
      ZooKeeper zk = Controller.getInstance().getZKInstance();
      String newName = oldName;
      if(blueprint.getUrl()!=null) {
        newName = ZookeeperUtil.getBaseURL(blueprint.getUrl().toString());
      } else {
        blueprint.setUrl(uri.getAbsolutePath().toURL());
      }
      byte[] data = JAXBUtil.write(blueprint);
      Stat stat = zk.exists(CommonConfigurationKeys.ZOOKEEPER_CONFIG_BLUEPRINT_PATH_DEFAULT+'/'+newName, false);
      if(stat!=null && oldName.equals(newName)) {
        // Update existing blueprint
        String path = CommonConfigurationKeys.ZOOKEEPER_CONFIG_BLUEPRINT_PATH_DEFAULT+'/'+oldName; 
        zk.delete(path, stat.getVersion());
        zk.create(CommonConfigurationKeys.ZOOKEEPER_CONFIG_BLUEPRINT_PATH_DEFAULT+'/'+newName, data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
      } else if(stat!=null) {
        // Conflict in name change
        throw new WebApplicationException(409);
      } else {
        // Create new blueprint
        try {
          zk.create(CommonConfigurationKeys.ZOOKEEPER_CONFIG_BLUEPRINT_PATH_DEFAULT+'/'+newName, data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch(KeeperException.NodeExistsException e) {
          throw new WebApplicationException(409);
        }
      }
      res = Response.noContent().build();
      return res;
    } catch(WebApplicationException e) {
      throw e;
    } catch(Exception e) {
      LOG.error(ExceptionUtil.getStackTrace(e));
      throw new WebApplicationException(500);
    }
  }
  
  @GET
  @Path("blueprint/{name}")
  public ConfigManifest getBlueprint(@PathParam("name") String name) {
    try {
      ZooKeeper zk = Controller.getInstance().getZKInstance();
      Stat current = new Stat();
      String path = ZookeeperUtil.getConfigManifestPath(name);
      byte[] data = zk.getData(path, false, current);
      ConfigManifest res = JAXBUtil.read(data, ConfigManifest.class);
      return res;
    } catch(Exception e) {
      LOG.error(ExceptionUtil.getStackTrace(e));
      throw new WebApplicationException(500);      
    }
  }
  
  @GET
  @Path("blueprint")
  public List<ConfigManifest> getList() {
    List<ConfigManifest> list = new ArrayList<ConfigManifest>();
    try {
      ZooKeeper zk = Controller.getInstance().getZKInstance();
      List<String> blueprints = zk.getChildren(CommonConfigurationKeys.ZOOKEEPER_CONFIG_BLUEPRINT_PATH_DEFAULT, false);
      Stat current = new Stat();
      for(String blueprint : blueprints) {
        byte[] data = zk.getData(ZookeeperUtil.getConfigManifestPath(blueprint), false, current);
        ConfigManifest x = JAXBUtil.read(data, ConfigManifest.class);
        list.add(x);
      }
    } catch(Exception e) {
      LOG.error(ExceptionUtil.getStackTrace(e));
      throw new WebApplicationException(500);
    }
    return list;
  }
  
  @DELETE
  @Path("blueprint/{name}")
  public Response deleteStack(@PathParam("name") String name) {
    ZooKeeper zk = Controller.getInstance().getZKInstance();
    try {
      String path = ZookeeperUtil.getConfigManifestPath(name);
      Stat current = zk.exists(path, false);
      zk.delete(path, current.getVersion());
    } catch(Exception e) {
      LOG.error(ExceptionUtil.getStackTrace(e));
      throw new WebApplicationException(500);      
    }
    Response res = Response.noContent().build();
    return res;
  }
}
