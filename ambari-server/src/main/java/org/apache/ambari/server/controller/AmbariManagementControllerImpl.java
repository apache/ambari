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

package org.apache.ambari.server.controller;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostAction;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostEvent;
import org.apache.ambari.server.state.ServiceComponentImpl;
import org.apache.ambari.server.state.StackVersion;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.ServiceImpl;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostImpl;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostInstallEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStartEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStopEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

public class AmbariManagementControllerImpl implements
    AmbariManagementController {

  private final static Logger LOG =
      LoggerFactory.getLogger(AmbariManagementControllerImpl.class);

  private final Clusters clusters;
  private final AtomicLong requestCounter;
  private String baseLogDir = "/tmp/ambari/";

  private final ActionManager actionManager;

  public AmbariManagementControllerImpl(@Assisted ActionManager actionManager
      , @Assisted Clusters clusters) {
    this.clusters = clusters;
    this.requestCounter = new AtomicLong();
    this.actionManager = actionManager;
  }

  @Override
  public TrackActionResponse createCluster(ClusterRequest request)
      throws AmbariException {
    if (request.getClusterName() == null
        || request.getClusterName().isEmpty()) {
      // TODO throw error
      throw new AmbariException("Invalid arguments");
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Received a createCluster request"
          + ", clusterName=" + request.getClusterName()
          + ", request=" + request);
    }

    // TODO validate stack version

    clusters.addCluster(request.getClusterName());

    if (request.getHostNames() != null) {
      clusters.mapHostsToCluster(request.getHostNames(),
          request.getClusterName());
    }

    Cluster c = clusters.getCluster(request.getClusterName());
    if (request.getStackVersion() != null) {
      c.setDesiredStackVersion(
          new StackVersion(request.getStackVersion()));
    }

    // TODO
    return null;
  }

  @Override
  public TrackActionResponse createService(ServiceRequest request)
      throws AmbariException {
    if (request.getClusterName() == null
        || request.getClusterName().isEmpty()) {
      // TODO throw error
      throw new AmbariException("Invalid arguments");
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Received a createService request"
          + ", clusterName=" + request.getClusterName()
          + ", serviceName=" + request.getServiceName()
          + ", request=" + request);
    }

    final Cluster cluster = clusters.getCluster(request.getClusterName());

    Service s = null;
    try {
      s = cluster.getService(request.getServiceName());
      if (s != null) {
        // TODO fix exception
        throw new AmbariException("Service already exists within cluster"
            + ", clusterName=" + cluster.getClusterName()
            + ", clusterId=" + cluster.getClusterId()
            + ", serviceName=" + s.getName());
      }
    } catch (AmbariException e) {
      // Expected
    }

    // TODO initialize configs based off service.configVersions
    Map<String, Config> configs = new HashMap<String, Config>();

    // Assuming service does not exist
    s = new ServiceImpl(cluster, request.getServiceName());

    // TODO validate correct desired state
    if (request.getDesiredState() != null
        && !request.getDesiredState().isEmpty()) {
      State state = State.valueOf(request.getDesiredState());
      if (!state.isValidDesiredState()
          || state != State.INIT) {
        // TODO fix
        throw new AmbariException("Invalid desired state");
      }

      s.setDesiredState(state);
    }
    s.updateConfigs(configs);
    s.setDesiredStackVersion(cluster.getDesiredStackVersion());
    cluster.addService(s);

    // TODO take action based on desired state
    // should we allow a non-INIT desired state?

    // TODO
    return null;
  }

  @Override
  public TrackActionResponse createComponent(ServiceComponentRequest request)
      throws AmbariException {
    if (request.getClusterName() == null
        || request.getClusterName().isEmpty()
        || request.getServiceName() == null
        || request.getServiceName().isEmpty()
        || request.getComponentName() == null
        || request.getComponentName().isEmpty()) {
      // TODO throw exception
      throw new AmbariException("Invalid arguments");
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Received a createComponent request"
          + ", clusterName=" + request.getClusterName()
          + ", serviceName=" + request.getServiceName()
          + ", componentName=" + request.getComponentName()
          + ", request=" + request);
    }

    final Cluster cluster = clusters.getCluster(request.getClusterName());
    final Service s = cluster.getService(request.getServiceName());
    ServiceComponent sc = null;
    try {
      sc = s.getServiceComponent(request.getComponentName());
      if (sc != null) {
        // TODO fix exception
        throw new AmbariException("ServiceComponent already exists"
            + " within cluster"
            + ", clusterName=" + cluster.getClusterName()
            + ", clusterId=" + cluster.getClusterId()
            + ", serviceName=" + s.getName()
            + ", serviceComponentName" + sc.getName());
      }
    } catch (AmbariException e) {
      // Expected
    }

    sc = new ServiceComponentImpl(s, request.getComponentName());
    sc.setDesiredStackVersion(s.getDesiredStackVersion());

    // TODO validate correct desired state
    if (request.getDesiredState() != null
        && !request.getDesiredState().isEmpty()) {
      State state = State.valueOf(request.getDesiredState());
      if (!state.isValidDesiredState()
          || state != State.INIT) {
        // TODO fix
        throw new AmbariException("Invalid desired state");
      }
      sc.setDesiredState(state);
    } else {
      sc.setDesiredState(s.getDesiredState());
    }

    // TODO fix config versions to configs conversion
    Map<String, Config> configs = new HashMap<String, Config>();
    if (request.getConfigVersions() == null) {
    } else {

    }

    sc.updateDesiredConfigs(configs);
    s.addServiceComponent(sc);

    // TODO
    return null;
  }

  @Override
  public TrackActionResponse createHost(HostRequest request)
      throws AmbariException {
    if (request.getHostname() == null
        || request.getHostname().isEmpty()) {
      // TODO throw error
      throw new AmbariException("Invalid arguments");
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Received a createHost request"
          + ", hostname=" + request.getHostname()
          + ", request=" + request);
    }

    Host h = clusters.getHost(request.getHostname());
    if (h == null) {
      // TODO throw exception as not bootstrapped
    }
    // TODO should agent registration create entry in the DB or should we
    // re-think schema to allow simple new entry

    clusters.addHost(request.getHostname());

    for (String clusterName : request.getClusterNames()) {
      clusters.mapHostToCluster(request.getHostname(), clusterName);
    }

    h = clusters.getHost(request.getHostname());
    h.setHostAttributes(request.getHostAttributes());

    // TODO
    return null;
  }

  @Override
  public TrackActionResponse createHostComponent(ServiceComponentHostRequest
      request) throws AmbariException {
    if (request.getClusterName() == null
        || request.getClusterName().isEmpty()
        || request.getServiceName() == null
        || request.getServiceName().isEmpty()
        || request.getComponentName() == null
        || request.getComponentName().isEmpty()
        || request.getHostname() == null
        || request.getHostname().isEmpty()) {
      // TODO throw error
      throw new AmbariException("Invalid arguments");
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Received a createHostComponent request"
          + ", clusterName=" + request.getClusterName()
          + ", serviceName=" + request.getServiceName()
          + ", componentName=" + request.getComponentName()
          + ", hostname=" + request.getHostname()
          + ", request=" + request);
    }

    final Cluster cluster = clusters.getCluster(request.getClusterName());
    final Service s = cluster.getService(request.getServiceName());
    final ServiceComponent sc = s.getServiceComponent(
        request.getComponentName());
    ServiceComponentHost sch = null;
    try {
      sch = sc.getServiceComponentHost(
          request.getHostname());
      if (sch != null) {
        // TODO fix exception
        throw new AmbariException("ServiceComponentHost already exists "
            + "within cluster"
            + ", clusterName=" + cluster.getClusterName()
            + ", clusterId=" + cluster.getClusterId()
            + ", serviceName=" + s.getName()
            + ", serviceComponentName" + sc.getName()
            + ", hostname=" + sch.getHostName());
      }
    } catch (AmbariException e) {
      // Expected
    }

    // TODO meta-data integration needed here
    // for now lets hack it up
    boolean isClient = true;
    if (sc.getName().equals("NAMENODE")
        || sc.getName().equals("DATANODE")
        || sc.getName().equals("SECONDARY_NAMENODE")) {
      isClient = false;
    }

    sch = new ServiceComponentHostImpl(sc, request.getHostname(), isClient);

    // TODO validate correct desired state
    if (request.getDesiredState() != null
        && !request.getDesiredState().isEmpty()) {
      State state = State.valueOf(request.getDesiredState());
      if (!state.isValidDesiredState()
          || state != State.INIT) {
        // TODO fix
        throw new AmbariException("Invalid desired state");
      }
      sch.setDesiredState(state);
    } else {
      sch.setDesiredState(sc.getDesiredState());
    }

    sch.setDesiredStackVersion(sc.getDesiredStackVersion());

    // TODO fix config versions to configs conversion
    Map<String, Config> configs = new HashMap<String, Config>();
    if (request.getConfigVersions() == null) {
    } else {

    }

    sch.updateDesiredConfigs(configs);
    sc.addServiceComponentHost(sch);

    // TODO handle action if desired state is something other than INIT
    if (State.INIT == sch.getDesiredState()) {
      // TODO fix
      // better way to say nothing to do?
      return null;
    } else {
      // TODO throw error?
      // only INIT allowed in create?
    }

    // if we go with the more complex approach
    // only need to check/compare desired state as these are new objects
    // uncomment out broken code from below and fix as needed


    // TODO better code needed
//    boolean installNeeded = false;
//    boolean startNeeded = false;
//    if (State.INSTALLED == sch.getDesiredState()) {
//      installNeeded = true;
//    } else if (State.STARTED == sch.getDesiredState()) {
//      installNeeded = true;
//      startNeeded = true;
//    } else {
//      // TODO throw error
//    }
//
//    long requestId = requestCounter.incrementAndGet();
//    String logDir = baseLogDir + "/" + requestId;
//
//    long now = System.currentTimeMillis();
//    List<Stage> stages = new ArrayList<Stage>();
//    if (installNeeded) {
//      Stage stage = new Stage(requestId, logDir, cluster.getClusterName());
//      HostAction ha = new HostAction(sch.getHostName());
//      HostRoleCommand cmd = new HostRoleCommand(sch.getServiceName(),
//          Role.valueOf(sch.getServiceComponentName()),
//          new ServiceComponentHostInstallEvent(sch.getServiceComponentName(),
//              sch.getHostName(), now));
//      ha.addHostRoleCommand(cmd);
//      stage.addHostAction(sch.getHostName(), ha);
//      stages.add(stage);
//    }
//    if (startNeeded) {
//      Stage stage = new Stage(requestId, logDir, cluster.getClusterName());
//      HostAction ha = new HostAction(sch.getHostName());
//      HostRoleCommand cmd = new HostRoleCommand(sch.getServiceName(),
//          Role.valueOf(sch.getServiceComponentName()),
//          new ServiceComponentHostInstallEvent(sch.getServiceComponentName(),
//              sch.getHostName(), now));
//      ha.addHostRoleCommand(cmd);
//      stage.addHostAction(sch.getHostName(), ha);
//      stages.add(stage);
//    }
//    actionManager.sendActions(stages);
//
//    return new TrackActionResponse(requestId);

    return null;
  }

  private Stage createNewStage(Cluster cluster, long requestId) {
    String logDir = baseLogDir + "/" + requestId;

    Stage stage = new Stage(requestId, logDir, cluster.getClusterName());
    return stage;
  }

  private HostAction createHostAction(Stage stage, ServiceComponentHost scHost,
      Map<String, Config> configs,
      ServiceComponentHostEvent event,
      long nowTimestamp) {

    HostAction ha = new HostAction(scHost.getHostName());

    HostRoleCommand cmd = new HostRoleCommand(scHost.getServiceName(),
        Role.valueOf(scHost.getServiceComponentName()),
        new ServiceComponentHostInstallEvent(scHost.getServiceComponentName(),
            scHost.getHostName(), nowTimestamp));


    ha.addHostRoleCommand(cmd);

    ExecutionCommand execCmd = ha.getCommandToHost();
    execCmd.setCommandId(stage.getActionId());
    execCmd.setClusterName(scHost.getClusterName());

    // Generate cluster host info
    // TODO fix - use something from somewhere to generate this at some point
    Map<String, List<String>> clusterHostInfo =
        new TreeMap<String, List<String>>();

    // TODO hack alert
    List<String> slaveHostList = new ArrayList<String>();
    slaveHostList.add("localhost");
    clusterHostInfo.put("slave_hosts", slaveHostList);
    execCmd.setClusterHostInfo(clusterHostInfo);

    // TODO do something from configs here
    Map<String, String> hdfsSite = new TreeMap<String, String>();
    hdfsSite.put("dfs.block.size", "2560000000");
    hdfsSite.put("magic_config_string", "magic_blah");
    Map<String, Map<String, String>> configurations =
        new TreeMap<String, Map<String, String>>();
    configurations.put("hdfs-site", hdfsSite);
    execCmd.setConfigurations(configurations);

    Map<String, String> params = new TreeMap<String, String>();
    params.put("magic_param", "/x/y/z");
    execCmd.setParams(params);

    Map<String, String> roleParams = new TreeMap<String, String>();
    roleParams.put("magic_role_param", "false");

    execCmd.addRoleCommand(scHost.getServiceComponentName(),
        event.getType().toString(), roleParams);

    return ha;
  }

  @Override
  public Set<ClusterResponse> getClusters(ClusterRequest request)
      throws AmbariException {
    Set<ClusterResponse> response = new HashSet<ClusterResponse>();

    if (request.getClusterName() != null) {
      Cluster c = clusters.getCluster(request.getClusterName());
      response.add(c.convertToResponse());
      return response;
    }

    Map<String, Cluster> allClusters = clusters.getClusters();
    for (Cluster c : allClusters.values()) {
      response.add(c.convertToResponse());
    }
    return response;
  }

  @Override
  public Set<ServiceResponse> getServices(ServiceRequest request)
      throws AmbariException {
    if (request.getClusterName() == null
        || request.getClusterName().isEmpty()) {
      // TODO fix throw error
      // or handle all possible searches for null properties
      throw new AmbariException("Invalid arguments");
    }
    final Cluster cluster = clusters.getCluster(request.getClusterName());

    Set<ServiceResponse> response = new HashSet<ServiceResponse>();
    if (request.getServiceName() != null) {
      Service s = cluster.getService(request.getServiceName());
      response.add(s.convertToResponse());
      return response;
    }

    // TODO support search on predicates?
    // filter based on desired state?

    Map<String, Service> allServices = cluster.getServices();
    for (Service s : allServices.values()) {
      response.add(s.convertToResponse());
    }
    return response;

  }

  @Override
  public Set<ServiceComponentResponse> getComponents(
      ServiceComponentRequest request) throws AmbariException {
    if (request.getClusterName() == null
        || request.getClusterName().isEmpty()
        || request.getServiceName() == null
        || request.getServiceName().isEmpty()) {
      // TODO fix throw error
      // or handle all possible searches for null properties
      throw new AmbariException("Invalid arguments");
    }

    final Cluster cluster = clusters.getCluster(request.getClusterName());
    final Service s = cluster.getService(request.getServiceName());

    Set<ServiceComponentResponse> response =
        new HashSet<ServiceComponentResponse>();
    if (request.getComponentName() != null) {
      ServiceComponent sc = s.getServiceComponent(request.getComponentName());
      response.add(sc.convertToResponse());
      return response;
    }

    Map<String, ServiceComponent> allSvcComps = s.getServiceComponents();
    for (ServiceComponent sc : allSvcComps.values()) {
      response.add(sc.convertToResponse());
    }
    return response;
  }

  @Override
  public Set<HostResponse> getHosts(HostRequest request)
      throws AmbariException {
    Set<HostResponse> response = new HashSet<HostResponse>();

    List<Host> hosts = null;
    if (request.getHostname() != null) {
      Host h = clusters.getHost(request.getHostname());
      hosts = new ArrayList<Host>();
      hosts.add(h);
    } else {
      hosts = clusters.getHosts();
    }

    for (Host h : hosts) {
      HostResponse r = h.convertToResponse();
      Set<Cluster> cs =
          clusters.getClustersForHost(request.getHostname());
      for (Cluster c : cs) {
        r.getClusterNames().add(c.getClusterName());
      }
      response.add(r);
    }
    return response;
  }

  @Override
  public Set<ServiceComponentHostResponse> getHostComponents(
      ServiceComponentHostRequest request) throws AmbariException {
    if (request.getClusterName() == null
        || request.getClusterName().isEmpty()
        || request.getServiceName() == null
        || request.getServiceName().isEmpty()
        || request.getComponentName() == null
        || request.getComponentName().isEmpty()) {
      // TODO fix throw error
      // or handle all possible searches for null properties
      throw new AmbariException("Invalid arguments");
    }

    final Cluster cluster = clusters.getCluster(request.getClusterName());
    final Service s = cluster.getService(request.getServiceName());
    final ServiceComponent sc = s.getServiceComponent(
        request.getComponentName());

    Set<ServiceComponentHostResponse> response =
        new HashSet<ServiceComponentHostResponse>();

    if (request.getHostname() != null) {
      ServiceComponentHost sch = sc.getServiceComponentHost(
          request.getHostname());
      ServiceComponentHostResponse r = sch.convertToResponse();
      response.add(r);
      return response;
    }

    Map<String, ServiceComponentHost> allSch = sc.getServiceComponentHosts();
    for (ServiceComponentHost sch : allSch.values()) {
      ServiceComponentHostResponse r = sch.convertToResponse();
      response.add(r);
    }

    return response;
  }

  @Override
  public TrackActionResponse updateCluster(ClusterRequest request)
      throws AmbariException {
    // for now only update host list supported
    if (request.getClusterName() == null
        || request.getClusterName().isEmpty()) {
      // TODO throw error
      throw new AmbariException("Invalid arguments");
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Received a updateCluster request"
          + ", clusterName=" + request.getClusterName()
          + ", request=" + request);
    }

    final Cluster c = clusters.getCluster(request.getClusterName());
    clusters.mapHostsToCluster(request.getHostNames(),
        request.getClusterName());

    if (!request.getStackVersion().equals(c.getDesiredStackVersion())) {
      throw new AmbariException("Update of desired stack version"
          + " not supported");
    }

    // TODO fix
    return null;
  }

  // TODO refactor code out of all update functions
  /*
  private TrackActionResponse triggerStateChange(State newState, Service s,
      ServiceComponent sc, ServiceComponentHost sch) {
    return null;
  }
  */

  @Override
  public TrackActionResponse updateService(ServiceRequest request)
      throws AmbariException {
    if (request.getClusterName() == null
        || request.getClusterName().isEmpty()
        || request.getServiceName() == null
        || request.getServiceName().isEmpty()) {
      // TODO throw error
      throw new AmbariException("Invalid arguments");
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Received a updateService request"
          + ", clusterName=" + request.getClusterName()
          + ", serviceName=" + request.getServiceName()
          + ", request=" + request);
    }

    final Cluster cluster = clusters.getCluster(request.getClusterName());
    final Service s = cluster.getService(request.getServiceName());

    if (request.getConfigVersions() != null) {
      // TODO
      // handle config updates
      // handle recursive updates to all components and hostcomponents
      // if different from old desired configs, trigger relevant actions
      throw new AmbariException("Unsupported operation - config updates not"
          + " allowed");
    }

    if (request.getDesiredState() == null) {
      // TODO fix return
      return null;
    }

    State newState = State.valueOf(request.getDesiredState());
    if (!newState.isValidDesiredState()) {
      // TODO fix
      throw new AmbariException("Invalid desired state");
    }


    State oldState = s.getDesiredState();
    if (newState == oldState) {
      // TODO fix return
      return null;
    }

    List<ServiceComponent> changedComps =
        new ArrayList<ServiceComponent>();
    Map<String, List<ServiceComponentHost>> changedScHosts =
        new HashMap<String, List<ServiceComponentHost>>();

    // TODO fix
    // currently everything is being done based on desired state
    // at some point do we need to do stuff based on live state?

    for (ServiceComponent sc : s.getServiceComponents().values()) {
      State oldScState = sc.getDesiredState();
      if (newState == oldScState) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Ignoring ServiceComponent"
              + ", clusterName=" + request.getClusterName()
              + ", serviceName=" + s.getName()
              + ", componentName=" + sc.getName()
              + ", currentState=" + sc.getDesiredState()
              + ", newState=" + newState);
        }
        continue;
      }
      LOG.debug("Handling update to ServiceComponent"
          + ", clusterName=" + request.getClusterName()
          + ", serviceName=" + s.getName()
          + ", componentName=" + sc.getName()
          + ", currentState=" + sc.getDesiredState()
          + ", newState=" + newState);
      changedComps.add(sc);
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        if (newState == sch.getDesiredState()) {
          LOG.debug("Ignoring ServiceComponentHost"
              + ", clusterName=" + request.getClusterName()
              + ", serviceName=" + s.getName()
              + ", componentName=" + sc.getName()
              + ", hostname=" + sch.getHostName()
              + ", currentState=" + sch.getDesiredState()
              + ", newState=" + newState);
          continue;
        }
        if (!changedScHosts.containsKey(sc.getName())) {
          changedScHosts.put(sc.getName(),
              new ArrayList<ServiceComponentHost>());
        }
        LOG.debug("Handling update to ServiceComponentHost"
            + ", clusterName=" + request.getClusterName()
            + ", serviceName=" + s.getName()
            + ", componentName=" + sc.getName()
            + ", hostname=" + sch.getHostName()
            + ", currentState=" + sch.getDesiredState()
            + ", newState=" + newState);
        changedScHosts.get(sc.getName()).add(sch);
      }
    }

    // TODO additional validation?

    // TODO order hostcomponents to determine stages

    // TODO lets continue hacking

    // assuming only HDFS
    List<String> orderedCompNames = new ArrayList<String>();
    orderedCompNames.add("NAMENODE");
    orderedCompNames.add("SECONDARY_NAMENODE");
    orderedCompNames.add("DATANODE");
    orderedCompNames.add("HDFS_CLIENT");

    long nowTimestamp = System.currentTimeMillis();
    long requestId = requestCounter.incrementAndGet();

    List<Stage> stages = new ArrayList<Stage>();
    long stageId = 0;
    for (String compName : orderedCompNames) {
      if (!changedScHosts.containsKey(compName)
          || changedScHosts.get(compName).isEmpty()) {
        continue;
      }
      Stage stage = createNewStage(cluster, requestId);
      stage.setStageId(++stageId);
      for (ServiceComponentHost scHost : changedScHosts.get(compName)) {
        Map<String, Config> configs = null;
        ServiceComponentHostEvent event;
        State oldSchState = scHost.getDesiredState();
        switch(newState) {
          case INSTALLED:
            if (oldSchState == State.INIT
                || oldSchState == State.UNINSTALLED
                || oldSchState == State.INSTALLED) {
              event = new ServiceComponentHostInstallEvent(
                  scHost.getServiceComponentName(),
                  scHost.getHostName(), nowTimestamp);
            } else if (oldSchState == State.STARTED) {
              event = new ServiceComponentHostStopEvent(
                  scHost.getServiceComponentName(),
                  scHost.getHostName(), nowTimestamp);
            } else {
              throw new AmbariException("Invalid transition"
                  + ", oldDesiredState=" + oldSchState
                  + ", newDesiredState" + newState);
            }
            break;
          case STARTED:
            if (oldSchState == State.INSTALLED) {
                event = new ServiceComponentHostStartEvent(
                    scHost.getServiceComponentName(),
                    scHost.getHostName(), nowTimestamp);
            } else {
              throw new AmbariException("Invalid transition"
                  + ", oldDesiredState=" + oldSchState
                  + ", newDesiredState" + newState);
            }
            break;
          default:
            // TODO fix handling other transitions
            throw new AmbariException("Unsupported state change operation");
        }

        HostAction ha = createHostAction(stage, scHost, configs, event,
            nowTimestamp);
        stage.addHostAction(scHost.getHostName(), ha);
      }
      if (LOG.isDebugEnabled()) {
        LOG.debug("Adding new stage for updateService request"
            + ", clusterName=" + request.getClusterName()
            + ", serviceName=" + request.getServiceName()
            + ", stage=" + stage);
      }
      stages.add(stage);
    }

    for (ServiceComponent sc : changedComps) {
      sc.setDesiredState(newState);
    }

    for (List<ServiceComponentHost> schosts : changedScHosts.values()) {
      for (ServiceComponentHost sch : schosts) {
        sch.setDesiredState(newState);
      }
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Triggering Action Manager"
          + ", clusterName=" + request.getClusterName()
          + ", serviceName=" + request.getServiceName()
          + ", stagesCount=" + stages.size());
    }
    actionManager.sendActions(stages);

    return new TrackActionResponse(requestId);
  }

  @Override
  public TrackActionResponse updateComponent(ServiceComponentRequest request)
      throws AmbariException {

    if (request.getClusterName() == null
        || request.getClusterName().isEmpty()
        || request.getServiceName() == null
        || request.getServiceName().isEmpty()
        || request.getComponentName() == null
        || request.getComponentName().isEmpty()) {
      // TODO fix throw error
      // or handle all possible searches for null properties
      throw new AmbariException("Invalid arguments");
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Received a updateComponent request"
          + ", clusterName=" + request.getClusterName()
          + ", serviceName=" + request.getServiceName()
          + ", componentName=" + request.getComponentName()
          + ", request=" + request);
    }

    final Cluster cluster = clusters.getCluster(request.getClusterName());
    final Service s = cluster.getService(request.getServiceName());
    final ServiceComponent sc = s.getServiceComponent(
        request.getComponentName());

    if (request.getConfigVersions() != null) {
      // TODO
      // handle config updates
      // handle recursive updates to all components and hostcomponents
      // if different from old desired configs, trigger relevant actions
      throw new AmbariException("Unsupported operation - config updates not"
          + " allowed");
    }

    if (request.getDesiredState() == null) {
      // TODO fix return
      return null;
    }

    State newState = State.valueOf(request.getDesiredState());
    if (!newState.isValidDesiredState()) {
      // TODO fix
      throw new AmbariException("Invalid desired state");
    }

    State oldState = sc.getDesiredState();
    if (newState == oldState) {
      // TODO fix return
      return null;
    }

    List<ServiceComponentHost> changedScHosts =
        new ArrayList<ServiceComponentHost>();

    // TODO fix
    // currently everything is being done based on desired state
    // at some point do we need to do stuff based on live state?

    for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
      if (newState == sch.getDesiredState()) {
        continue;
      }
      changedScHosts.add(sch);
    }

    // TODO additional validation?

    // TODO order hostcomponents to determine stages

    // TODO lets continue hacking

    long nowTimestamp = System.currentTimeMillis();
    long requestId = requestCounter.incrementAndGet();
    long stageId = 0;
    List<Stage> stages = new ArrayList<Stage>();
    Stage stage = createNewStage(cluster, requestId);
    stage.setStageId(++stageId);
    for (ServiceComponentHost scHost : changedScHosts) {
      Map<String, Config> configs = null;
      ServiceComponentHostEvent event;
      State oldSchState = scHost.getDesiredState();
      switch(newState) {
        case INSTALLED:
          if (oldSchState == State.INIT
              || oldSchState == State.UNINSTALLED
              || oldSchState == State.INSTALLED) {
            event = new ServiceComponentHostInstallEvent(
                scHost.getServiceComponentName(),
                scHost.getHostName(), nowTimestamp);
          } else if (oldSchState == State.STARTED) {
            event = new ServiceComponentHostStopEvent(
                scHost.getServiceComponentName(),
                scHost.getHostName(), nowTimestamp);
          } else {
            throw new AmbariException("Invalid transition"
                + ", oldDesiredState=" + oldSchState
                + ", newDesiredState" + newState);
          }
          break;
        case STARTED:
          if (oldSchState == State.INSTALLED) {
              event = new ServiceComponentHostStartEvent(
                  scHost.getServiceComponentName(),
                  scHost.getHostName(), nowTimestamp);
          } else {
            throw new AmbariException("Invalid transition"
                + ", oldDesiredState=" + oldSchState
                + ", newDesiredState" + newState);
          }
          break;
        default:
          // TODO fix handling other transitions
          throw new AmbariException("Unsupported state change operation");
      }

      HostAction ha = createHostAction(stage, scHost, configs, event,
          nowTimestamp);
      stage.addHostAction(scHost.getHostName(), ha);
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Adding new stage for updateComponent request"
          + ", clusterName=" + request.getClusterName()
          + ", serviceName=" + request.getServiceName()
          + ", componentName=" + request.getComponentName()
          + ", stage=" + stage);
    }
    stages.add(stage);

    for (ServiceComponentHost sch : changedScHosts) {
      sch.setDesiredState(newState);
    }

    actionManager.sendActions(stages);

    return new TrackActionResponse(requestId);

  }

  @Override
  public TrackActionResponse updateHost(HostRequest request)
      throws AmbariException {
    if (request.getHostname() == null
        || request.getHostname().isEmpty()) {
      // TODO throw error
      throw new AmbariException("Invalid arguments");
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Received a updateHost request"
          + ", hostname=" + request.getHostname()
          + ", request=" + request);
    }

    Host h = clusters.getHost(request.getHostname());
    if (h == null) {
      // TODO throw exception as not created
    }

    for (String clusterName : request.getClusterNames()) {
      clusters.mapHostToCluster(request.getHostname(), clusterName);
    }

    h.setHostAttributes(request.getHostAttributes());

    // TODO
    return null;
  }

  @Override
  public TrackActionResponse updateHostComponent(
      ServiceComponentHostRequest request) throws AmbariException {

    if (request.getClusterName() == null
        || request.getClusterName().isEmpty()
        || request.getServiceName() == null
        || request.getServiceName().isEmpty()
        || request.getComponentName() == null
        || request.getComponentName().isEmpty()
        || request.getHostname() == null
        || request.getHostname().isEmpty()) {
      // TODO throw error
      throw new AmbariException("Invalid arguments");
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Received a createHostComponent request"
          + ", clusterName=" + request.getClusterName()
          + ", serviceName=" + request.getServiceName()
          + ", componentName=" + request.getComponentName()
          + ", hostname=" + request.getHostname()
          + ", request=" + request);
    }

    final Cluster cluster = clusters.getCluster(request.getClusterName());
    final Service s = cluster.getService(request.getServiceName());
    final ServiceComponent sc = s.getServiceComponent(
        request.getComponentName());
    final ServiceComponentHost sch = sc.getServiceComponentHost(
        request.getHostname());

    if (request.getConfigVersions() != null) {
      // TODO
      // handle config updates
      // handle recursive updates to all components and hostcomponents
      // if different from old desired configs, trigger relevant actions
      throw new AmbariException("Unsupported operation - config updates not"
          + " allowed");
    }

    if (request.getDesiredState() == null) {
      // TODO fix return
      return null;
    }

    State newState = State.valueOf(request.getDesiredState());
    if (!newState.isValidDesiredState()) {
      // TODO fix
      throw new AmbariException("Invalid desired state");
    }

    State oldState = sch.getDesiredState();
    if (newState == oldState) {
      // TODO fix return
      return null;
    }

    // TODO fix
    // currently everything is being done based on desired state
    // at some point do we need to do stuff based on live state?

    // TODO additional validation?

    // TODO order hostcomponents to determine stages

    // TODO lets continue hacking

    long nowTimestamp = System.currentTimeMillis();
    long requestId = requestCounter.incrementAndGet();
    long stageId = 0;

    List<Stage> stages = new ArrayList<Stage>();
    Stage stage = createNewStage(cluster, requestId);
    stage.setStageId(++stageId);

    Map<String, Config> configs = null;
    ServiceComponentHostEvent event;
    State oldSchState = sch.getDesiredState();
    switch(newState) {
      case INSTALLED:
        if (oldSchState == State.INIT
            || oldSchState == State.UNINSTALLED
            || oldSchState == State.INSTALLED) {
          event = new ServiceComponentHostInstallEvent(
              sch.getServiceComponentName(),
              sch.getHostName(), nowTimestamp);
        } else if (oldSchState == State.STARTED) {
          event = new ServiceComponentHostStopEvent(
              sch.getServiceComponentName(),
              sch.getHostName(), nowTimestamp);
        } else {
          throw new AmbariException("Invalid transition"
              + ", oldDesiredState=" + oldSchState
              + ", newDesiredState" + newState);
        }
        break;
      case STARTED:
        if (oldSchState == State.INSTALLED) {
            event = new ServiceComponentHostStartEvent(
                sch.getServiceComponentName(),
                sch.getHostName(), nowTimestamp);
        } else {
          throw new AmbariException("Invalid transition"
              + ", oldDesiredState=" + oldSchState
              + ", newDesiredState" + newState);
        }
        break;
      default:
        // TODO fix handling other transitions
        throw new AmbariException("Unsupported state change operation");
    }

    HostAction ha = createHostAction(stage, sch, configs, event,
        nowTimestamp);
    stage.addHostAction(sch.getHostName(), ha);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Adding new stage for createHostComponent request"
          + ", clusterName=" + request.getClusterName()
          + ", serviceName=" + request.getServiceName()
          + ", componentName=" + request.getComponentName()
          + ", hostname=" + request.getHostname()
          + ", stage=" + stage);
    }
    stages.add(stage);

    sch.setDesiredState(newState);

    actionManager.sendActions(stages);

    return new TrackActionResponse(requestId);
  }

  @Override
  public TrackActionResponse deleteCluster(ClusterRequest request)
      throws AmbariException {
    // TODO not implemented yet
    throw new AmbariException("Delete cluster not supported");
  }

  @Override
  public TrackActionResponse deleteService(ServiceRequest request)
      throws AmbariException {
    // TODO not implemented yet
    throw new AmbariException("Delete service not supported");
  }

  @Override
  public TrackActionResponse deleteComponent(ServiceComponentRequest request)
      throws AmbariException {
    // TODO not implemented yet
    throw new AmbariException("Delete component not supported");
  }

  @Override
  public TrackActionResponse deleteHost(HostRequest request)
      throws AmbariException {
    // TODO not implemented yet
    throw new AmbariException("Delete host not supported");
  }

  @Override
  public TrackActionResponse deleteHostComponent(
      ServiceComponentHostRequest request) throws AmbariException {
    // TODO not implemented yet
    throw new AmbariException("Delete host component not supported");
  }
}
