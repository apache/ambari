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

import com.google.inject.Injector;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.ServiceComponentHostNotFoundException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.metadata.RoleCommandOrder;
import org.apache.ambari.server.stageplanner.RoleGraph;
import org.apache.ambari.server.state.*;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostInstallEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStartEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStopEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

@Singleton
public class AmbariManagementControllerImpl implements
    AmbariManagementController {

  private final static Logger LOG =
      LoggerFactory.getLogger(AmbariManagementControllerImpl.class);

  private final Clusters clusters;

  private String baseLogDir = "/tmp/ambari/";

  private final ActionManager actionManager;

  private final Injector injector;

  private static RoleCommandOrder rco;
  static {
    rco = new RoleCommandOrder();
    RoleCommandOrder.initialize();
  }

  @Inject
  private ServiceFactory serviceFactory;
  @Inject
  private ServiceComponentFactory serviceComponentFactory;
  @Inject
  private ServiceComponentHostFactory serviceComponentHostFactory;

  @Inject
  public AmbariManagementControllerImpl(ActionManager actionManager,
      Clusters clusters, Injector injector) {
    this.clusters = clusters;
    this.actionManager = actionManager;
    this.injector = injector;
    injector.injectMembers(this);
    LOG.info("Initializing the AmbariManagementControllerImpl");
  }

  @Override
  public void createCluster(ClusterRequest request)
      throws AmbariException {
    if (request.getClusterName() == null
        || request.getClusterName().isEmpty()) {
      // FIXME throw correct error
      throw new AmbariException("Invalid arguments");
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Received a createCluster request"
          + ", clusterName=" + request.getClusterName()
          + ", request=" + request);
    }

    // FIXME validate stack version
    // FIXME add support for desired configs at cluster level

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

  }

  @Override
  public void createServices(Set<ServiceRequest> requests)
      throws AmbariException {

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
      return;
    }

    // do all validation checks
    Map<String, Set<String>> serviceNames = new HashMap<String, Set<String>>();
    Set<String> duplicates = new HashSet<String>();
    for (ServiceRequest request : requests) {
      if (request.getClusterName() == null
          || request.getClusterName().isEmpty()
          || request.getServiceName() == null
          || request.getServiceName().isEmpty()) {
        // FIXME throw correct error
        throw new AmbariException("Invalid arguments"
            + ", clustername and servicename should be non-null and non-empty");
      }

      if (LOG.isDebugEnabled()) {
        LOG.debug("Received a createService request"
            + ", clusterName=" + request.getClusterName()
            + ", serviceName=" + request.getServiceName()
            + ", request=" + request);
      }

      if (!serviceNames.containsKey(request.getClusterName())) {
        serviceNames.put(request.getClusterName(), new HashSet<String>());
      }
      if (serviceNames.get(request.getClusterName())
          .contains(request.getServiceName())) {
        // throw error later for dup
        duplicates.add(request.getServiceName());
        continue;
      }
      serviceNames.get(request.getClusterName()).add(request.getServiceName());

      if (request.getDesiredState() != null
          && !request.getDesiredState().isEmpty()) {
        State state = State.valueOf(request.getDesiredState());
        if (!state.isValidDesiredState()
            || state != State.INIT) {
          // FIXME throw correct error
          throw new AmbariException("Invalid desired state"
              + " only INIT state allowed during creation"
              + ", providedDesiredState=" + request.getDesiredState());
        }
      }

      Cluster cluster = clusters.getCluster(request.getClusterName());
      try {
        Service s = cluster.getService(request.getServiceName());
        if (s != null) {
          // throw error later for dup
          duplicates.add(request.getServiceName());
          continue;
        }
      } catch (AmbariException e) {
        // Expected
      }
    }

    // ensure only a single cluster update
    if (serviceNames.size() != 1) {
      // FIXME throw correct error
      throw new AmbariException("Invalid arguments - updates allowed only on"
          + " one cluster at a time");
    }

    // Validate dups
    if (!duplicates.isEmpty()) {
      StringBuilder svcNames = new StringBuilder();
      boolean first = true;
      for (String svcName : duplicates) {
        if (!first) {
          svcNames.append(",");
        }
        first = false;
        svcNames.append(svcName);
      }
      // FIXME throw correct error
      throw new AmbariException("Invalid request"
          + " contains duplicates within request or already existing services"
          + ", duplicateServiceNames=" + svcNames.toString());
    }

    // now to the real work
    for (ServiceRequest request : requests) {
      Cluster cluster = clusters.getCluster(request.getClusterName());

      // TODO initialize configs based off service.configVersions
      Map<String, Config> configs = new HashMap<String, Config>();

      State state = State.INIT;
      if (request.getDesiredState() != null
          && !request.getDesiredState().isEmpty()) {
        state = State.valueOf(request.getDesiredState());
      }

      // Already checked that service does not exist
      Service s = serviceFactory.createNew(cluster, request.getServiceName());

      s.setDesiredState(state);
      s.updateDesiredConfigs(configs);
      s.setDesiredStackVersion(cluster.getDesiredStackVersion());
      cluster.addService(s);
      s.persist();
    }

  }

  @Override
  public void createComponents(
      Set<ServiceComponentRequest> requests) throws AmbariException {

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
      return;
    }

    // do all validation checks
    Map<String, Map<String, Set<String>>> componentNames =
        new HashMap<String, Map<String,Set<String>>>();
    Set<String> duplicates = new HashSet<String>();

    for (ServiceComponentRequest request : requests) {
      if (request.getClusterName() == null
          || request.getClusterName().isEmpty()
          || request.getServiceName() == null
          || request.getServiceName().isEmpty()
          || request.getComponentName() == null
          || request.getComponentName().isEmpty()) {
        // FIXME throw correct error
        throw new AmbariException("Invalid arguments"
            + ", clustername, servicename and componentname should be"
            + " non-null and non-empty");
      }

      if (LOG.isDebugEnabled()) {
        LOG.debug("Received a createComponent request"
            + ", clusterName=" + request.getClusterName()
            + ", serviceName=" + request.getServiceName()
            + ", componentName=" + request.getComponentName()
            + ", request=" + request);
      }

      if (!componentNames.containsKey(request.getClusterName())) {
        componentNames.put(request.getClusterName(),
            new HashMap<String, Set<String>>());
      }
      if (!componentNames.get(request.getClusterName())
          .containsKey(request.getServiceName())) {
        componentNames.get(request.getClusterName()).put(
            request.getServiceName(), new HashSet<String>());
      }
      if (componentNames.get(request.getClusterName())
          .get(request.getServiceName()).contains(request.getComponentName())){
        // throw error later for dup
        duplicates.add(request.getServiceName()
            + "-" + request.getComponentName());
        continue;
      }
      componentNames.get(request.getClusterName())
          .get(request.getServiceName()).add(request.getComponentName());

      if (request.getDesiredState() != null
          && !request.getDesiredState().isEmpty()) {
        State state = State.valueOf(request.getDesiredState());
        if (!state.isValidDesiredState()
            || state != State.INIT) {
          // FIXME throw correct error
          throw new AmbariException("Invalid desired state"
              + " only INIT state allowed during creation"
              + ", providedDesiredState=" + request.getDesiredState());
        }
      }

      Cluster cluster = clusters.getCluster(request.getClusterName());
      Service s = cluster.getService(request.getServiceName());
      try {
        ServiceComponent sc = s.getServiceComponent(request.getComponentName());
        if (sc != null) {
          // throw error later for dup
          duplicates.add(request.getServiceName()
              + "-" + request.getComponentName());
          continue;
        }
      } catch (AmbariException e) {
        // Expected
      }

    }

    // ensure only a single cluster update
    if (componentNames.size() != 1) {
      // FIXME throw correct error
      throw new AmbariException("Invalid arguments - updates allowed only one"
          + " cluster at a time");
    }

    // Validate dups
    if (!duplicates.isEmpty()) {
      StringBuilder names = new StringBuilder();
      boolean first = true;
      for (String cName : duplicates) {
        if (!first) {
          names.append(",");
        }
        first = false;
        names.append(cName);
      }
      // FIXME throw correct error
      throw new AmbariException("Invalid request"
          + " contains duplicates within request or"
          + " already existing service components"
          + ", duplicateServiceComponentsNames=" + names.toString());
    }


    // now doing actual work
    for (ServiceComponentRequest request : requests) {
      Cluster cluster = clusters.getCluster(request.getClusterName());
      Service s = cluster.getService(request.getServiceName());
      ServiceComponent sc = serviceComponentFactory.createNew(s,
          request.getComponentName());
      sc.setDesiredStackVersion(s.getDesiredStackVersion());

      if (request.getDesiredState() != null
          && !request.getDesiredState().isEmpty()) {
        State state = State.valueOf(request.getDesiredState());
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
      sc.persist();
    }

  }

  @Override
  public void createHosts(Set<HostRequest> requests)
      throws AmbariException {

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
      return;
    }

    Set<String> duplicates = new HashSet<String>();
    Set<String> unknowns = new HashSet<String>();
    Set<String> allHosts = new HashSet<String>();
    for (HostRequest request : requests) {
      if (request.getHostname() == null
          || request.getHostname().isEmpty()) {
        // FIXME throw correct error
        throw new AmbariException("Invalid arguments");
      }

      if (LOG.isDebugEnabled()) {
        LOG.debug("Received a createHost request"
            + ", hostname=" + request.getHostname()
            + ", request=" + request);
      }

      if (allHosts.contains(request.getHostname())) {
        // throw dup error later
        duplicates.add(request.getHostname());
        continue;
      }
      allHosts.add(request.getHostname());

      try {
        // ensure host is registered
        clusters.getHost(request.getHostname());
      }
      catch (HostNotFoundException e) {
        unknowns.add(request.getHostname());
        continue;
      }
    }

    if (!duplicates.isEmpty()) {
      StringBuilder names = new StringBuilder();
      boolean first = true;
      for (String hName : duplicates) {
        if (!first) {
          names.append(",");
        }
        first = false;
        names.append(hName);
      }
      // FIXME throw correct error
      throw new AmbariException("Invalid request contains duplicate hostnames"
          + ", hostnames=" + names.toString());
    }

    if (!unknowns.isEmpty()) {
      StringBuilder names = new StringBuilder();
      boolean first = true;
      for (String hName : unknowns) {
        if (!first) {
          names.append(",");
        }
        first = false;
        names.append(hName);
      }
      // FIXME throw correct error
      throw new AmbariException("Some hosts have not been registered with"
          + " the server"
          + ", hostnames=" + names.toString());
    }

    for (HostRequest request : requests) {
      Host h = clusters.getHost(request.getHostname());
      if (request.getClusterNames() != null) {
        for (String clusterName : request.getClusterNames()) {
          clusters.mapHostToCluster(request.getHostname(), clusterName);
        }
      }

      if (request.getHostAttributes() != null) {
        h = clusters.getHost(request.getHostname());
        h.setHostAttributes(request.getHostAttributes());
      }
    }
  }

  @Override
  public void createHostComponents(Set<ServiceComponentHostRequest> requests)
      throws AmbariException {

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
      return;
    }

    // do all validation checks
    Map<String, Map<String, Map<String, Set<String>>>> hostComponentNames =
        new HashMap<String, Map<String, Map<String, Set<String>>>>();
    Set<String> duplicates = new HashSet<String>();
    for (ServiceComponentHostRequest request : requests) {
      if (request.getClusterName() == null
          || request.getClusterName().isEmpty()
          || request.getComponentName() == null
          || request.getComponentName().isEmpty()
          || request.getHostname() == null
          || request.getHostname().isEmpty()) {
        // FIXME throw correct error
        throw new AmbariException("Invalid arguments");
      }

      // FIXME Hard coded stuff --- needs to be fixed.
      if (request.getServiceName() == null
          || request.getServiceName().isEmpty()
        ) {
        // FIXME get service name from meta data lib?
        // Major boo-boo if component names are not unique
        request.setServiceName("HDFS");
      }

      if (LOG.isDebugEnabled()) {
        LOG.debug("Received a createHostComponent request"
            + ", clusterName=" + request.getClusterName()
            + ", serviceName=" + request.getServiceName()
            + ", componentName=" + request.getComponentName()
            + ", hostname=" + request.getHostname()
            + ", request=" + request);
      }

      if (!hostComponentNames.containsKey(request.getClusterName())) {
        hostComponentNames.put(request.getClusterName(),
            new HashMap<String, Map<String,Set<String>>>());
      }
      if (!hostComponentNames.get(request.getClusterName())
          .containsKey(request.getServiceName())) {
        hostComponentNames.get(request.getClusterName()).put(
            request.getServiceName(), new HashMap<String, Set<String>>());
      }
      if (!hostComponentNames.get(request.getClusterName())
          .get(request.getServiceName())
          .containsKey(request.getComponentName())) {
        hostComponentNames.get(request.getClusterName())
            .get(request.getServiceName()).put(request.getComponentName(),
                new HashSet<String>());
      }
      if (hostComponentNames.get(request.getClusterName())
          .get(request.getServiceName()).get(request.getComponentName())
          .contains(request.getHostname())) {
        duplicates.add(request.getServiceName()
            + "-" + request.getComponentName()
            + "-" + request.getHostname());
        continue;
      }
      hostComponentNames.get(request.getClusterName())
          .get(request.getServiceName()).get(request.getComponentName())
          .add(request.getHostname());

      if (request.getDesiredState() != null
          && !request.getDesiredState().isEmpty()) {
        State state = State.valueOf(request.getDesiredState());
        if (!state.isValidDesiredState()
            || state != State.INIT) {
          // FIXME throw correct error
          throw new AmbariException("Invalid desired state"
              + " only INIT state allowed during creation"
              + ", providedDesiredState=" + request.getDesiredState());
        }
      }

      Cluster cluster = clusters.getCluster(request.getClusterName());
      Service s = cluster.getService(request.getServiceName());
      ServiceComponent sc = s.getServiceComponent(
          request.getComponentName());
      Host host = clusters.getHost(request.getHostname());
      Set<Cluster> mappedClusters =
          clusters.getClustersForHost(host.getHostName());
      boolean validCluster = false;
      for (Cluster mappedCluster : mappedClusters) {
        if (mappedCluster.getClusterName().equals(
            request.getClusterName())) {
          validCluster = true;
          break;
        }
      }
      if (!validCluster) {
        // TODO fix throw correct error
        throw new AmbariException("Invalid request as host does not belong to"
            + " given cluster"
            + ", clusterName=" + request.getClusterName()
            + ", serviceName=" + request.getServiceName()
            + ", componentName=" + request.getComponentName()
            + ", hostname=" + request.getHostname());
      }
      try {
        ServiceComponentHost sch = sc.getServiceComponentHost(
            request.getHostname());
        if (sch != null) {
          duplicates.add(request.getServiceName()
              + "-" + request.getComponentName()
              + "-" + request.getHostname());
          continue;
        }
      } catch (AmbariException e) {
        // Expected
      }
    }

    // ensure only a single cluster update
    if (hostComponentNames.size() != 1) {
      // FIXME throw correct error
      throw new AmbariException("Invalid arguments - updates allowed only one"
          + " cluster at a time");
    }

    if (!duplicates.isEmpty()) {
      StringBuilder names = new StringBuilder();
      boolean first = true;
      for (String hName : duplicates) {
        if (!first) {
          names.append(",");
        }
        first = false;
        names.append(hName);
      }
      // FIXME throw correct error
      throw new AmbariException("Invalid request"
          + " contains duplicates within request or"
          + " already existing host components"
          + ", duplicateServiceComponentHostNames=" + names.toString());
    }

    // now doing actual work
    for (ServiceComponentHostRequest request : requests) {
      Cluster cluster = clusters.getCluster(request.getClusterName());
      Service s = cluster.getService(request.getServiceName());
      ServiceComponent sc = s.getServiceComponent(
          request.getComponentName());

      // FIXME meta-data integration needed here
      // for now lets hack it up
      boolean isClient = true;
      if (sc.getName().equals("NAMENODE")
          || sc.getName().equals("DATANODE")
          || sc.getName().equals("SECONDARY_NAMENODE")) {
        isClient = false;
      }

      ServiceComponentHost sch =
          serviceComponentHostFactory.createNew(sc, request.getHostname(),
              isClient);

      // TODO validate correct desired state
      if (request.getDesiredState() != null
          && !request.getDesiredState().isEmpty()) {
        State state = State.valueOf(request.getDesiredState());
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
      sch.persist();
    }

  }

  private Stage createNewStage(Cluster cluster, long requestId) {
    String logDir = baseLogDir + "/" + requestId;

    Stage stage = new Stage(requestId, logDir, cluster.getClusterName());
    return stage;
  }

  private void createHostAction(Stage stage, ServiceComponentHost scHost,
      Map<String, Config> configs,
      RoleCommand command,
      long nowTimestamp,
      ServiceComponentHostEvent event) {

    stage.addHostRoleExecutionCommand(scHost.getHostName(), Role.valueOf(scHost
        .getServiceComponentName()), command,
        event, scHost.getClusterName(),
        scHost.getServiceName());
    ExecutionCommand execCmd = stage.getExecutionCommand(scHost.getHostName(),
        scHost.getServiceComponentName());
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
    hdfsSite.put("fs.default.name", "hdfs://localhost:50070");
    hdfsSite.put("dfs.block.size", "2560000000");
    hdfsSite.put("magic_config_string", "magic_blah");
    Map<String, Map<String, String>> configurations =
        new TreeMap<String, Map<String, String>>();
    configurations.put("hdfs-site", hdfsSite);
    execCmd.setConfigurations(configurations);

    Map<String, String> params = new TreeMap<String, String>();
    params.put("magic_param", "/x/y/z");
    execCmd.setHostLevelParams(params);

    Map<String, String> roleParams = new TreeMap<String, String>();
    roleParams.put("magic_role_param", "false");

    execCmd.setRoleParams(roleParams);

    return;
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

    // FIXME validate stack version if not null

    Map<String, Cluster> allClusters = clusters.getClusters();
    for (Cluster c : allClusters.values()) {
      if (request.getStackVersion() != null
          && !request.getStackVersion().isEmpty()) {
        if (!request.getStackVersion().equals(
            c.getDesiredStackVersion().getStackVersion())) {
          // skip non matching stack versions
          continue;
        }
      }
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

    boolean checkDesiredState = false;
    State desiredStateToCheck = null;
    if (request.getDesiredState() != null
        && !request.getDesiredState().isEmpty()) {
      desiredStateToCheck = State.valueOf(request.getDesiredState());
      if (!desiredStateToCheck.isValidDesiredState()) {
        // FIXME throw correct error
        throw new AmbariException("Invalid arguments");
      }
      checkDesiredState = true;
    }

    for (Service s : cluster.getServices().values()) {
      if (checkDesiredState
          && (desiredStateToCheck != s.getDesiredState())) {
        // skip non matching state
        continue;
      }
      response.add(s.convertToResponse());
    }
    return response;

  }

  @Override
  public Set<ServiceComponentResponse> getComponents(
      ServiceComponentRequest request) throws AmbariException {
    if (request.getClusterName() == null
        || request.getClusterName().isEmpty()) {
      // TODO fix throw error
      throw new AmbariException("Invalid arguments");
    }

    final Cluster cluster = clusters.getCluster(request.getClusterName());

    Set<ServiceComponentResponse> response =
        new HashSet<ServiceComponentResponse>();

    if (request.getComponentName() != null) {
      if (request.getServiceName() == null) {
        // TODO fix throw error
        throw new AmbariException("Invalid arguments");
      }
      Service s = cluster.getService(request.getServiceName());
      ServiceComponent sc = s.getServiceComponent(request.getComponentName());
      response.add(sc.convertToResponse());
      return response;
    }

    boolean checkDesiredState = false;
    State desiredStateToCheck = null;
    if (request.getDesiredState() != null
        && !request.getDesiredState().isEmpty()) {
      desiredStateToCheck = State.valueOf(request.getDesiredState());
      if (!desiredStateToCheck.isValidDesiredState()) {
        // FIXME throw correct error
        throw new AmbariException("Invalid arguments");
      }
      checkDesiredState = true;
    }

    Set<Service> services = new HashSet<Service>();
    if (request.getServiceName() != null
        && !request.getServiceName().isEmpty()) {
      services.add(cluster.getService(request.getServiceName()));
    } else {
      services.addAll(cluster.getServices().values());
    }

    for (Service s : services) {
      // filter on request.getDesiredState()
      for (ServiceComponent sc : s.getServiceComponents().values()) {
        if (checkDesiredState
            && (desiredStateToCheck != sc.getDesiredState())) {
          // skip non matching state
          continue;
        }
        response.add(sc.convertToResponse());
      }
    }
    return response;
  }

  @Override
  public Set<HostResponse> getHosts(HostRequest request)
      throws AmbariException {
    Set<HostResponse> response = new HashSet<HostResponse>();

    // FIXME what is the requirement for filtering on host attributes?

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
        || request.getClusterName().isEmpty()) {
      // TODO fix throw error
      // or handle all possible searches for null properties
      throw new AmbariException("Invalid arguments");
    }

    final Cluster cluster = clusters.getCluster(request.getClusterName());

    if (request.getComponentName() != null) {
      if (request.getServiceName() == null) {
        // FIXME get service name from meta data or throw exception??
        // for now using a brute force search across all services
      }
    }

    Set<Service> services = new HashSet<Service>();
    if (request.getServiceName() != null
        && !request.getServiceName().isEmpty()) {
      services.add(cluster.getService(request.getServiceName()));
    } else {
      services.addAll(cluster.getServices().values());
    }

    Set<ServiceComponentHostResponse> response =
        new HashSet<ServiceComponentHostResponse>();

    boolean checkDesiredState = false;
    State desiredStateToCheck = null;
    if (request.getDesiredState() != null
        && !request.getDesiredState().isEmpty()) {
      desiredStateToCheck = State.valueOf(request.getDesiredState());
      if (!desiredStateToCheck.isValidDesiredState()) {
        // FIXME throw correct error
        throw new AmbariException("Invalid arguments");
      }
      checkDesiredState = true;
    }

    for (Service s : services) {
      // filter on component name if provided
      Set<ServiceComponent> components = new HashSet<ServiceComponent>();
      // FIXME hack for now as we need to filter on name until meta data layer
      // integration happens
      // at that point, only a single component object should be looked at
      components.addAll(s.getServiceComponents().values());
      for(ServiceComponent sc : components) {
        if (request.getComponentName() != null) {
          if (!sc.getName().equals(request.getComponentName())) {
            // FIXME for
            continue;
          }
        }

        // filter on hostname if provided
        // filter on desired state if provided

        if (request.getHostname() != null) {
          try {
            ServiceComponentHost sch = sc.getServiceComponentHost(
                request.getHostname());
            if (checkDesiredState
                && (desiredStateToCheck != sch.getDesiredState())) {
              continue;
            }
            ServiceComponentHostResponse r = sch.convertToResponse();
            response.add(r);
          } catch (ServiceComponentHostNotFoundException e) {
            // Expected
            // ignore and continue
          }
        } else {
          for (ServiceComponentHost sch :
              sc.getServiceComponentHosts().values()) {
            if (checkDesiredState
                && (desiredStateToCheck != sch.getDesiredState())) {
              continue;
            }
            ServiceComponentHostResponse r = sch.convertToResponse();
            response.add(r);
          }
        }
      }
    }
    return response;
  }

  @Override
  public TrackActionResponse updateCluster(ClusterRequest request)
      throws AmbariException {
    // for now only update host list supported
    if (request.getClusterName() == null
        || request.getClusterName().isEmpty()) {
      // FIXME throw correct error
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
      // FIXME throw correct error
      throw new AmbariException("Update of desired stack version"
          + " not supported");
    }

    // TODO fix
    return null;
  }

  // FIXME refactor code out of all update functions
  /*
  private TrackActionResponse triggerStateChange(State newState, Service s,
      ServiceComponent sc, ServiceComponentHost sch) {
    return null;
  }
  */

  private TrackActionResponse doStageCreation(Cluster cluster,
      Map<State, List<Service>> changedServices,
      Map<State, List<ServiceComponent>> changedComps,
      Map<String, Map<State, List<ServiceComponentHost>>> changedScHosts)
          throws AmbariException {

    // TODO handle different transitions?
    // Say HDFS to stopped and MR to started, what order should actions be done
    // in?

    // TODO additional validation?
    // verify all configs
    // verify all required components

    // TODO order hostcomponents to determine stages

    // TODO lets continue hacking

    long nowTimestamp = System.currentTimeMillis();
    long requestId = actionManager.getNextRequestId();

    // FIXME cannot work with a single stage
    long stageId = 0;
    Stage stage = createNewStage(cluster, requestId);
    stage.setStageId(stageId);
    for (String compName : changedScHosts.keySet()) {
      for (State newState : changedScHosts.get(compName).keySet()) {
        for (ServiceComponentHost scHost :
            changedScHosts.get(compName).get(newState)) {
          Map<String, Config> configs = null;
          RoleCommand roleCommand;
          State oldSchState = scHost.getDesiredState();
          ServiceComponentHostEvent event;
          switch(newState) {
            case INSTALLED:
              if (oldSchState == State.INIT
                  || oldSchState == State.UNINSTALLED
                  || oldSchState == State.INSTALLED) {
                roleCommand = RoleCommand.INSTALL;
                event = new ServiceComponentHostInstallEvent(
                    scHost.getServiceComponentName(), scHost.getHostName(),
                    nowTimestamp);
              } else if (oldSchState == State.STARTED) {
                roleCommand = RoleCommand.STOP;
                event = new ServiceComponentHostStopEvent(
                    scHost.getServiceComponentName(), scHost.getHostName(),
                    nowTimestamp);
              } else {
                // FIXME throw correct error
                throw new AmbariException("Invalid transition"
                    + ", oldDesiredState=" + oldSchState
                    + ", newDesiredState" + newState);
              }
              break;
            case STARTED:
              if (oldSchState == State.INSTALLED) {
                roleCommand = RoleCommand.START;
                event = new ServiceComponentHostStartEvent(
                    scHost.getServiceComponentName(), scHost.getHostName(),
                    nowTimestamp);
              } else {
                // FIXME throw correct error
                throw new AmbariException("Invalid transition"
                    + ", oldDesiredState=" + oldSchState
                    + ", newDesiredState" + newState);
              }
              break;
            default:
              // TODO fix handling other transitions
              throw new AmbariException("Unsupported state change operation");
          }

          if (LOG.isDebugEnabled()) {
            LOG.debug("Create a new host action"
                + ", requestId=" + requestId
                + ", componentName=" + scHost.getServiceComponentName()
                + ", hostname=" + scHost.getHostName()
                + ", roleCommand=" + roleCommand.name());
          }
          createHostAction(stage, scHost, configs, roleCommand,
            nowTimestamp, event);
        }
      }
    }

    if (changedServices != null) {
      for (Entry<State, List<Service>> entry : changedServices.entrySet()) {
        State newState = entry.getKey();
        for (Service s : entry.getValue()) {
          s.setDesiredState(newState);
        }
      }
    }

    if (changedComps != null) {
      for (Entry<State, List<ServiceComponent>> entry : changedComps.entrySet()){
        State newState = entry.getKey();
        for (ServiceComponent sc : entry.getValue()) {
          sc.setDesiredState(newState);
        }
      }
    }

    for (Map<State, List<ServiceComponentHost>> stateScHostMap :
        changedScHosts.values()) {
      for (Entry<State, List<ServiceComponentHost>> entry :
          stateScHostMap.entrySet()) {
        State newState = entry.getKey();
        for (ServiceComponentHost sch : entry.getValue()) {
          sch.setDesiredState(newState);
        }
      }
    }

    RoleGraph rg = new RoleGraph(rco);
    rg.build(stage);
    List<Stage> stages = rg.getStages();

    if (LOG.isDebugEnabled()) {
      LOG.debug("Triggering Action Manager"
          + ", clusterName=" + cluster.getClusterName()
          + ", requestId=" + requestId
          + ", stagesCount=" + stages.size());
    }
    actionManager.sendActions(stages);

    return new TrackActionResponse(requestId);

  }


  @Override
  public TrackActionResponse updateServices(Set<ServiceRequest> requests)
      throws AmbariException {

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
      // FIXME return val
      return null;
    }

    Map<State, List<Service>> changedServices
      = new HashMap<State, List<Service>>();
    Map<State, List<ServiceComponent>> changedComps =
        new HashMap<State, List<ServiceComponent>>();
    Map<String, Map<State, List<ServiceComponentHost>>> changedScHosts =
        new HashMap<String, Map<State, List<ServiceComponentHost>>>();

    Set<String> clusterNames = new HashSet<String>();
    Map<String, Set<String>> serviceNames = new HashMap<String, Set<String>>();
    Set<State> seenNewStates = new HashSet<State>();

    for (ServiceRequest request : requests) {
      if (request.getClusterName() == null
          || request.getClusterName().isEmpty()
          || request.getServiceName() == null
          || request.getServiceName().isEmpty()) {
        // FIXME throw correct error
        throw new AmbariException("Invalid arguments");
      }

      if (LOG.isDebugEnabled()) {
        LOG.debug("Received a updateService request"
            + ", clusterName=" + request.getClusterName()
            + ", serviceName=" + request.getServiceName()
            + ", request=" + request);
      }

      // FIXME need to do dup validation checks

      clusterNames.add(request.getClusterName());

      if (clusterNames.size() > 1) {
        // FIXME throw correct error
        throw new AmbariException("Updates to multiple clusters is not"
            + " supported");
      }

      if (!serviceNames.containsKey(request.getClusterName())) {
        serviceNames.put(request.getClusterName(), new HashSet<String>());
      }
      if (serviceNames.get(request.getClusterName())
          .contains(request.getServiceName())) {
        // FIXME throw correct error
        // FIXME throw single exception
        throw new AmbariException("Invalid request contains duplicate"
            + " service names");
      }
      serviceNames.get(request.getClusterName()).add(request.getServiceName());

      // FIXME validate valid services
      Cluster cluster = clusters.getCluster(request.getClusterName());
      Service s = cluster.getService(request.getServiceName());

      if (request.getConfigVersions() != null) {
        // TODO handle config updates
        // handle recursive updates to all components and hostcomponents
        // if different from old desired configs, trigger relevant actions
        throw new AmbariException("Unsupported operation - config updates not"
            + " allowed");
      }

      if (request.getDesiredState() == null
          || request.getDesiredState().isEmpty()) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Nothing to do for new updateService request"
              + ", clusterName=" + request.getClusterName()
              + ", serviceName=" + request.getServiceName()
              + ", newDesiredState=null");
        }
        continue;
      }

      State newState = State.valueOf(request.getDesiredState());
      if (!newState.isValidDesiredState()) {
        // FIXME fix with appropriate exception
        throw new AmbariException("Invalid desired state");
      }

      seenNewStates.add(newState);

      State oldState = s.getDesiredState();
      if (newState != oldState) {
        if (!changedServices.containsKey(newState)) {
          changedServices.put(newState, new ArrayList<Service>());
        }
        changedServices.get(newState).add(s);
      }

      // should we check whether all servicecomponents and
      // servicecomponenthosts are in the required desired state?
      // For now, checking as no live state comparison

      // TODO fix usage of live state
      // currently everything is being done based on desired state
      // at some point do we need to do stuff based on live state?

      for (ServiceComponent sc : s.getServiceComponents().values()) {
        State oldScState = sc.getDesiredState();
        if (newState != oldScState) {
          if (!changedComps.containsKey(newState)) {
            changedComps.put(newState, new ArrayList<ServiceComponent>());
          }
          changedComps.get(newState).add(sc);
        }
        if (LOG.isDebugEnabled()) {
          LOG.debug("Handling update to ServiceComponent"
              + ", clusterName=" + request.getClusterName()
              + ", serviceName=" + s.getName()
              + ", componentName=" + sc.getName()
              + ", currentState=" + sc.getDesiredState()
              + ", newState=" + newState);
        }
        for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
          if (newState == sch.getDesiredState()) {
            if (LOG.isDebugEnabled()) {
              LOG.debug("Ignoring ServiceComponentHost"
                  + ", clusterName=" + request.getClusterName()
                  + ", serviceName=" + s.getName()
                  + ", componentName=" + sc.getName()
                  + ", hostname=" + sch.getHostName()
                  + ", currentState=" + sch.getDesiredState()
                  + ", newState=" + newState);
            }
            continue;
          }
          if (!changedScHosts.containsKey(sc.getName())) {
            changedScHosts.put(sc.getName(),
                new HashMap<State, List<ServiceComponentHost>>());
          }
          if (!changedScHosts.get(sc.getName()).containsKey(newState)) {
            changedScHosts.get(sc.getName()).put(newState,
                new ArrayList<ServiceComponentHost>());
          }
          if (LOG.isDebugEnabled()) {
            LOG.debug("Handling update to ServiceComponentHost"
                + ", clusterName=" + request.getClusterName()
                + ", serviceName=" + s.getName()
                + ", componentName=" + sc.getName()
                + ", hostname=" + sch.getHostName()
                + ", currentState=" + sch.getDesiredState()
                + ", newState=" + newState);
          }
          changedScHosts.get(sc.getName()).get(newState).add(sch);
        }
      }
    }

    if (seenNewStates.size() > 1) {
      // FIXME should we handle this scenario
      throw new AmbariException("Cannot handle different desired state changes"
          + " for a set of services at the same time");
    }

    // TODO fix dup handling for multiple services in requests

    Cluster cluster = clusters.getCluster(clusterNames.iterator().next());

    return doStageCreation(cluster, changedServices,
        changedComps, changedScHosts);
  }

  @Override
  public TrackActionResponse updateComponents(
      Set<ServiceComponentRequest> requests) throws AmbariException {

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
      // FIXME fix return val
      return null;
    }

    Map<State, List<ServiceComponent>> changedComps =
        new HashMap<State, List<ServiceComponent>>();
    Map<String, Map<State, List<ServiceComponentHost>>> changedScHosts =
        new HashMap<String, Map<State, List<ServiceComponentHost>>>();

    Set<String> clusterNames = new HashSet<String>();
    Map<String, Map<String, Set<String>>> componentNames =
        new HashMap<String, Map<String,Set<String>>>();
    Set<State> seenNewStates = new HashSet<State>();

    for (ServiceComponentRequest request : requests) {
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

      // FIXME need to do dup validation checks

      clusterNames.add(request.getClusterName());

      if (clusterNames.size() > 1) {
        // FIXME throw correct error
        throw new AmbariException("Updates to multiple clusters is not"
            + " supported");
      }

      if (!componentNames.containsKey(request.getClusterName())) {
        componentNames.put(request.getClusterName(),
            new HashMap<String, Set<String>>());
      }
      if (!componentNames.get(request.getClusterName())
          .containsKey(request.getServiceName())) {
        componentNames.get(request.getClusterName()).put(
            request.getServiceName(), new HashSet<String>());
      }
      if (componentNames.get(request.getClusterName())
          .get(request.getServiceName()).contains(request.getComponentName())){
        // throw error later for dup
        throw new AmbariException("Invalid request contains duplicate"
            + " service components");
      }
      componentNames.get(request.getClusterName())
          .get(request.getServiceName()).add(request.getComponentName());

      // FIXME validate valid service components


      Cluster cluster = clusters.getCluster(request.getClusterName());
      Service s = cluster.getService(request.getServiceName());
      ServiceComponent sc = s.getServiceComponent(
        request.getComponentName());

      if (request.getConfigVersions() != null) {
        // TODO handle config updates
        // handle recursive updates to all components and hostcomponents
        // if different from old desired configs, trigger relevant actions
        throw new AmbariException("Unsupported operation - config updates not"
            + " allowed");
      }

      if (request.getDesiredState() == null
          || request.getDesiredState().isEmpty()) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Nothing to do for new updateServiceComponent request"
              + ", clusterName=" + request.getClusterName()
              + ", serviceName=" + request.getServiceName()
              + ", componentName=" + request.getComponentName()
              + ", newDesiredState=null");
        }
        continue;
      }

      State newState = State.valueOf(request.getDesiredState());
      if (!newState.isValidDesiredState()) {
        // TODO fix
        throw new AmbariException("Invalid desired state");
      }

      seenNewStates.add(newState);

      State oldState = sc.getDesiredState();
      if (newState != oldState) {
        if (!changedComps.containsKey(newState)) {
          changedComps.put(newState, new ArrayList<ServiceComponent>());
        }
        if (LOG.isDebugEnabled()) {
          LOG.debug("Handling update to ServiceComponent"
              + ", clusterName=" + request.getClusterName()
              + ", serviceName=" + s.getName()
              + ", componentName=" + sc.getName()
              + ", currentState=" + sc.getDesiredState()
              + ", newState=" + newState);
        }
        changedComps.get(newState).add(sc);
      }

      // TODO fix
      // currently everything is being done based on desired state
      // at some point do we need to do stuff based on live state?

      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        if (newState == sch.getDesiredState()) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Ignoring ServiceComponentHost"
                + ", clusterName=" + request.getClusterName()
                + ", serviceName=" + s.getName()
                + ", componentName=" + sc.getName()
                + ", hostname=" + sch.getHostName()
                + ", currentState=" + sch.getDesiredState()
                + ", newState=" + newState);
          }
          continue;
        }
        if (!changedScHosts.containsKey(sc.getName())) {
          changedScHosts.put(sc.getName(),
              new HashMap<State, List<ServiceComponentHost>>());
        }
        if (!changedScHosts.get(sc.getName()).containsKey(newState)) {
          changedScHosts.get(sc.getName()).put(newState,
              new ArrayList<ServiceComponentHost>());
        }
        if (LOG.isDebugEnabled()) {
          LOG.debug("Handling update to ServiceComponentHost"
              + ", clusterName=" + request.getClusterName()
              + ", serviceName=" + s.getName()
              + ", componentName=" + sc.getName()
              + ", hostname=" + sch.getHostName()
              + ", currentState=" + sch.getDesiredState()
              + ", newState=" + newState);
        }
        changedScHosts.get(sc.getName()).get(newState).add(sch);
      }
    }

    if (seenNewStates.size() > 1) {
      // FIXME should we handle this scenario
      throw new AmbariException("Cannot handle different desired state changes"
          + " for a set of service components at the same time");
    }

    // TODO additional validation?

    Cluster cluster = clusters.getCluster(clusterNames.iterator().next());

    return doStageCreation(cluster, null,
        changedComps, changedScHosts);
  }

  @Override
  public void updateHosts(Set<HostRequest> requests)
      throws AmbariException {

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
      return;
    }

    for (HostRequest request : requests) {
      if (request.getHostname() == null
          || request.getHostname().isEmpty()) {
        // FIXME throw correct error
        throw new AmbariException("Invalid arguments");
      }

      if (LOG.isDebugEnabled()) {
        LOG.debug("Received a updateHost request"
            + ", hostname=" + request.getHostname()
            + ", request=" + request);
      }

      Host h = clusters.getHost(request.getHostname());

      for (String clusterName : request.getClusterNames()) {
        clusters.mapHostToCluster(request.getHostname(), clusterName);
      }

      h.setHostAttributes(request.getHostAttributes());
    }

  }

  @Override
  public TrackActionResponse updateHostComponents(
      Set<ServiceComponentHostRequest> requests) throws AmbariException {

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
      // FIXME fix return val
      return null;
    }

    Map<String, Map<State, List<ServiceComponentHost>>> changedScHosts =
        new HashMap<String, Map<State, List<ServiceComponentHost>>>();

    Set<String> clusterNames = new HashSet<String>();
    Map<String, Map<String, Map<String, Set<String>>>> hostComponentNames =
        new HashMap<String, Map<String, Map<String, Set<String>>>>();
    Set<State> seenNewStates = new HashSet<State>();

    for (ServiceComponentHostRequest request : requests) {
      if (request.getClusterName() == null
          || request.getClusterName().isEmpty()
          || request.getServiceName() == null
          || request.getServiceName().isEmpty()
          || request.getComponentName() == null
          || request.getComponentName().isEmpty()
          || request.getHostname() == null
          || request.getHostname().isEmpty()) {
        // FIXME throw correct error
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

      // FIXME need to do dup validation checks

      clusterNames.add(request.getClusterName());

      if (clusterNames.size() > 1) {
        // FIXME throw correct error
        throw new AmbariException("Updates to multiple clusters is not"
            + " supported");
      }

      if (!hostComponentNames.containsKey(request.getClusterName())) {
        hostComponentNames.put(request.getClusterName(),
            new HashMap<String, Map<String,Set<String>>>());
      }
      if (!hostComponentNames.get(request.getClusterName())
          .containsKey(request.getServiceName())) {
        hostComponentNames.get(request.getClusterName()).put(
            request.getServiceName(), new HashMap<String, Set<String>>());
      }
      if (!hostComponentNames.get(request.getClusterName())
          .get(request.getServiceName())
          .containsKey(request.getComponentName())) {
        hostComponentNames.get(request.getClusterName())
            .get(request.getServiceName()).put(request.getComponentName(),
                new HashSet<String>());
      }
      if (hostComponentNames.get(request.getClusterName())
          .get(request.getServiceName()).get(request.getComponentName())
          .contains(request.getHostname())) {
        // FIXME throw correct error
        throw new AmbariException("Invalid request contains duplicate"
            + " hostcomponents");
      }
      hostComponentNames.get(request.getClusterName())
          .get(request.getServiceName()).get(request.getComponentName())
          .add(request.getHostname());


      Cluster cluster = clusters.getCluster(request.getClusterName());
      Service s = cluster.getService(request.getServiceName());
      ServiceComponent sc = s.getServiceComponent(
        request.getComponentName());
      ServiceComponentHost sch = sc.getServiceComponentHost(
        request.getHostname());

      if (request.getConfigVersions() != null) {
        // TODO handle config updates
        // handle recursive updates to all components and hostcomponents
        // if different from old desired configs, trigger relevant actions
        throw new AmbariException("Unsupported operation - config updates not"
            + " allowed");
      }

      if (request.getDesiredState() == null
          || request.getDesiredState().isEmpty()) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Nothing to do for new updateServiceComponentHost request"
              + ", clusterName=" + request.getClusterName()
              + ", serviceName=" + request.getServiceName()
              + ", componentName=" + request.getComponentName()
              + ", hostname=" + request.getHostname()
              + ", newDesiredState=null");
        }
        continue;
      }

      State newState = State.valueOf(request.getDesiredState());
      if (!newState.isValidDesiredState()) {
        // TODO fix
        throw new AmbariException("Invalid desired state");
      }

      seenNewStates.add(newState);

      State oldState = sch.getDesiredState();
      if (newState == oldState) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Ignoring ServiceComponentHost"
              + ", clusterName=" + request.getClusterName()
              + ", serviceName=" + s.getName()
              + ", componentName=" + sc.getName()
              + ", hostname=" + sch.getHostName()
              + ", currentState=" + sch.getDesiredState()
              + ", newState=" + newState);
        }
        continue;
      }

      if (!changedScHosts.containsKey(sc.getName())) {
        changedScHosts.put(sc.getName(),
            new HashMap<State, List<ServiceComponentHost>>());
      }
      if (!changedScHosts.get(sc.getName()).containsKey(newState)) {
        changedScHosts.get(sc.getName()).put(newState,
            new ArrayList<ServiceComponentHost>());
      }
      if (LOG.isDebugEnabled()) {
        LOG.debug("Handling update to ServiceComponentHost"
            + ", clusterName=" + request.getClusterName()
            + ", serviceName=" + s.getName()
            + ", componentName=" + sc.getName()
            + ", hostname=" + sch.getHostName()
            + ", currentState=" + sch.getDesiredState()
            + ", newState=" + newState);
      }
      changedScHosts.get(sc.getName()).get(newState).add(sch);
    }

    if (seenNewStates.size() > 1) {
      // FIXME should we handle this scenario
      throw new AmbariException("Cannot handle different desired state changes"
          + " for a set of service components at the same time");
    }

    // TODO fix
    // currently everything is being done based on desired state
    // at some point do we need to do stuff based on live state?

    // TODO additional validation?

    Cluster cluster = clusters.getCluster(clusterNames.iterator().next());

    return doStageCreation(cluster, null,
        null, changedScHosts);
  }

  @Override
  public TrackActionResponse deleteCluster(ClusterRequest request)
      throws AmbariException {
    // TODO not implemented yet
    throw new AmbariException("Delete cluster not supported");
  }

  @Override
  public TrackActionResponse deleteServices(Set<ServiceRequest> request)
      throws AmbariException {
    // TODO Auto-generated method stub
    throw new AmbariException("Delete services not supported");
  }

  @Override
  public TrackActionResponse deleteComponents(
      Set<ServiceComponentRequest> request) throws AmbariException {
    // TODO Auto-generated method stub
    throw new AmbariException("Delete components not supported");
  }

  @Override
  public TrackActionResponse deleteHosts(Set<HostRequest> request)
      throws AmbariException {
    throw new AmbariException("Delete hosts not supported");
  }

  @Override
  public TrackActionResponse deleteHostComponents(
      Set<ServiceComponentHostRequest> request) throws AmbariException {
    // TODO Auto-generated method stub
    throw new AmbariException("Delete host components not supported");
  }

}
