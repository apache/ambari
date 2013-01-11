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

import java.net.InetAddress;
import java.util.*;
import java.util.Map.Entry;

import org.apache.ambari.server.*;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.RequestStatus;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.metadata.RoleCommandOrder;
import org.apache.ambari.server.security.authorization.User;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.stageplanner.RoleGraph;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostEvent;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostInstallEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpInProgressEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStartEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStopEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

@Singleton
public class AmbariManagementControllerImpl implements
    AmbariManagementController {

  private final static Logger LOG =
      LoggerFactory.getLogger(AmbariManagementControllerImpl.class);

  private final Clusters clusters;

  private String baseLogDir = "/tmp/ambari/";

  private final ActionManager actionManager;

  @SuppressWarnings("unused")
  private final Injector injector;

  private final Gson gson;

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
  private ConfigFactory configFactory;
  @Inject
  private StageFactory stageFactory;
  @Inject
  private ActionMetadata actionMetadata;
  @Inject
  private AmbariMetaInfo ambariMetaInfo;
  @Inject
  private Users users;

  final private String masterHostname;

  final private static String JDK_RESOURCE_LOCATION =
      "/resources/";
 
  final private String jdkResourceUrl;

  @Inject
  public AmbariManagementControllerImpl(ActionManager actionManager,
      Clusters clusters, Injector injector) throws Exception {
    this.clusters = clusters;
    this.actionManager = actionManager;
    this.injector = injector;
    injector.injectMembers(this);
    this.gson = injector.getInstance(Gson.class);
    LOG.info("Initializing the AmbariManagementControllerImpl");
    this.masterHostname =  InetAddress.getLocalHost().getCanonicalHostName();
    this.jdkResourceUrl = "http://" + masterHostname + ":"
        + AmbariServer.getResourcesPort()
        + JDK_RESOURCE_LOCATION;
  }

  @Override
  public void createCluster(ClusterRequest request)
      throws AmbariException {
    if (request.getClusterName() == null
        || request.getClusterName().isEmpty()
        || request.getClusterId() != null) {
      throw new IllegalArgumentException("Cluster name should be provided" +
          " and clusterId should be null");
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Received a createCluster request"
          + ", clusterName=" + request.getClusterName()
          + ", request=" + request);
    }

    if (request.getStackVersion() == null
        || request.getStackVersion().isEmpty()) {
      throw new IllegalArgumentException("Stack information should be"
          + " provided when creating a cluster");
    }
    StackId stackId = new StackId(request.getStackVersion());
    StackInfo stackInfo = ambariMetaInfo.getStackInfo(stackId.getStackName(),
        stackId.getStackVersion());
    if (stackInfo == null) {
      throw new StackNotFoundException(stackId.getStackName(),
          stackId.getStackVersion());
    }

    // FIXME add support for desired configs at cluster level

    boolean foundInvalidHosts = false;
    StringBuilder invalidHostsStr = new StringBuilder();
    if (request.getHostNames() != null) {
      for (String hostname : request.getHostNames()) {
        try {
          clusters.getHost(hostname);
        } catch (HostNotFoundException e) {
          if (foundInvalidHosts) {
            invalidHostsStr.append(",");
          }
          foundInvalidHosts = true;
          invalidHostsStr.append(hostname);
        }
      }
    }
    if (foundInvalidHosts) {
      throw new HostNotFoundException(invalidHostsStr.toString());
    }

    clusters.addCluster(request.getClusterName());
    Cluster c = clusters.getCluster(request.getClusterName());
    if (request.getStackVersion() != null) {
      c.setDesiredStackVersion(
          new StackId(request.getStackVersion()));
    }

    if (request.getHostNames() != null) {
      clusters.mapHostsToCluster(request.getHostNames(),
          request.getClusterName());
    }

  }

  @Override
  public synchronized void createServices(Set<ServiceRequest> requests)
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
        throw new IllegalArgumentException("Cluster name and service name"
            + " should be provided when creating a service");
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
          throw new IllegalArgumentException("Invalid desired state"
              + " only INIT state allowed during creation"
              + ", providedDesiredState=" + request.getDesiredState());
        }
      }

      Cluster cluster;
      try {
        cluster = clusters.getCluster(request.getClusterName());
      } catch (ClusterNotFoundException e) {
        throw new ParentObjectNotFoundException("Attempted to add a service to a cluster which doesn't exist", e);
      }
      try {
        Service s = cluster.getService(request.getServiceName());
        if (s != null) {
          // throw error later for dup
          duplicates.add(request.getServiceName());
          continue;
        }
      } catch (ServiceNotFoundException e) {
        // Expected
      }

      StackId stackId = cluster.getDesiredStackVersion();
      if (!ambariMetaInfo.isValidService(stackId.getStackName(),
          stackId.getStackVersion(), request.getServiceName())) {
        throw new IllegalArgumentException("Unsupported or invalid service"
            + " in stack"
            + ", clusterName=" + request.getClusterName()
            + ", serviceName=" + request.getServiceName()
            + ", stackInfo=" + stackId.getStackId());
      }
    }

    // ensure only a single cluster update
    if (serviceNames.size() != 1) {
      throw new IllegalArgumentException("Invalid arguments, updates allowed"
          + "on only one cluster at a time");
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
      String clusterName = requests.iterator().next().getClusterName();
      String msg;
      if (duplicates.size() == 1) {
        msg = "Attempted to create a service which already exists: "
            + ", clusterName=" + clusterName  + " serviceName=" + svcNames.toString();
      } else {
        msg = "Attempted to create services which already exist: "
            + ", clusterName=" + clusterName  + " serviceNames=" + svcNames.toString();
      }
      throw new DuplicateResourceException(msg);
    }

    // now to the real work
    for (ServiceRequest request : requests) {
      Cluster cluster = clusters.getCluster(request.getClusterName());

      // FIXME initialize configs based off service.configVersions
      Map<String, Config> configs = new HashMap<String, Config>();

      State state = State.INIT;

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
  public synchronized void createComponents(
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
          || request.getComponentName() == null
          || request.getComponentName().isEmpty()) {
        throw new IllegalArgumentException("Invalid arguments"
            + ", clustername and componentname should be"
            + " non-null and non-empty when trying to create a"
            + " component");
      }

      Cluster cluster;
      try {
        cluster = clusters.getCluster(request.getClusterName());
      } catch (ClusterNotFoundException e) {
        throw new ParentObjectNotFoundException(
            "Attempted to add a component to a cluster which doesn't exist:", e);
      }

      if (request.getServiceName() == null
          || request.getServiceName().isEmpty()) {
        StackId stackId = cluster.getDesiredStackVersion();
        String serviceName =
            ambariMetaInfo.getComponentToService(stackId.getStackName(),
                stackId.getStackVersion(), request.getComponentName());
        if (LOG.isDebugEnabled()) {
          LOG.debug("Looking up service name for component"
              + ", componentName=" + request.getComponentName()
              + ", serviceName=" + serviceName);
        }

        if (serviceName == null
            || serviceName.isEmpty()) {
          throw new AmbariException("Could not find service for component"
              + ", componentName=" + request.getComponentName()
              + ", clusterName=" + cluster.getClusterName()
              + ", stackInfo=" + stackId.getStackId());
        }
        request.setServiceName(serviceName);
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
        duplicates.add("[clusterName=" + request.getClusterName() + ", serviceName=" + request.getServiceName() +
            ", componentName=" + request.getComponentName() + "]");
        continue;
      }
      componentNames.get(request.getClusterName())
          .get(request.getServiceName()).add(request.getComponentName());

      if (request.getDesiredState() != null
          && !request.getDesiredState().isEmpty()) {
        State state = State.valueOf(request.getDesiredState());
        if (!state.isValidDesiredState()
            || state != State.INIT) {
          throw new IllegalArgumentException("Invalid desired state"
              + " only INIT state allowed during creation"
              + ", providedDesiredState=" + request.getDesiredState());
        }
      }

      Service s;
      try {
        s = cluster.getService(request.getServiceName());
      } catch (ServiceNotFoundException e) {
        throw new ParentObjectNotFoundException(
            "Attempted to add a component to a service which doesn't exist:", e);
      }
      try {
        ServiceComponent sc = s.getServiceComponent(request.getComponentName());
        if (sc != null) {
          // throw error later for dup
          duplicates.add("[clusterName=" + request.getClusterName() + ", serviceName=" + request.getServiceName() +
              ", componentName=" + request.getComponentName() + "]");
          continue;
        }
      } catch (AmbariException e) {
        // Expected
      }

      StackId stackId = s.getDesiredStackVersion();
      if (!ambariMetaInfo.isValidServiceComponent(stackId.getStackName(),
          stackId.getStackVersion(), s.getName(), request.getComponentName())) {
        throw new IllegalArgumentException("Unsupported or invalid component"
            + " in stack"
            + ", clusterName=" + request.getClusterName()
            + ", serviceName=" + request.getServiceName()
            + ", componentName=" + request.getComponentName()
            + ", stackInfo=" + stackId.getStackId());
      }
    }

    // ensure only a single cluster update
    if (componentNames.size() != 1) {
      throw new IllegalArgumentException("Invalid arguments, updates allowed"
          + "on only one cluster at a time");
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
      String msg;
      if (duplicates.size() == 1) {
        msg = "Attempted to create a component which already exists: ";
      } else {
        msg = "Attempted to create components which already exist: ";
      }
      throw new DuplicateResourceException(msg + names.toString());
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

      // FIXME fix config versions to configs conversion
      Map<String, Config> configs = new HashMap<String, Config>();
      if (request.getConfigVersions() != null) {
      }

      sc.updateDesiredConfigs(configs);
      s.addServiceComponent(sc);
      sc.persist();
    }

  }

  @Override
  public synchronized void createHosts(Set<HostRequest> requests)
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
        throw new IllegalArgumentException("Invalid arguments, hostname"
            + " cannot be null");
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

      if (request.getClusterName() != null) {
        try {
          // validate that cluster_name is valid
          clusters.getCluster(request.getClusterName());
        } catch (ClusterNotFoundException e) {
          throw new ParentObjectNotFoundException("Attempted to add a host to a cluster which doesn't exist: "
              + " clusterName=" + request.getClusterName());
        }
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
      throw new IllegalArgumentException("Invalid request contains"
          + " duplicate hostnames"
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

      throw new IllegalArgumentException("Attempted to add unknown hosts to a cluster.  " +
          "These hosts have not been registered with the server: " + names.toString());
    }

    for (HostRequest request : requests) {
      if (request.getClusterName() != null) {
        clusters.mapHostToCluster(request.getHostname(), request.getClusterName());
      }

      if (request.getHostAttributes() != null) {
        clusters.getHost(request.getHostname()).
            setHostAttributes(request.getHostAttributes());
      }
    }
  }

  @Override
  public synchronized void createHostComponents(Set<ServiceComponentHostRequest> requests)
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
        throw new IllegalArgumentException("Invalid arguments,"
            + " clustername, componentname and hostname should not be null"
            + " when trying to create a hostcomponent");
      }

      Cluster cluster;
      try {
        cluster = clusters.getCluster(request.getClusterName());
      } catch (ClusterNotFoundException e) {
        throw new ParentObjectNotFoundException(
            "Attempted to add a host_component to a cluster which doesn't exist: ", e);
      }

      if (request.getServiceName() == null
          || request.getServiceName().isEmpty()) {
        StackId stackId = cluster.getDesiredStackVersion();
        String serviceName =
            ambariMetaInfo.getComponentToService(stackId.getStackName(),
                stackId.getStackVersion(), request.getComponentName());
        if (LOG.isDebugEnabled()) {
          LOG.debug("Looking up service name for component"
              + ", componentName=" + request.getComponentName()
              + ", serviceName=" + serviceName);
        }
        if (serviceName == null
            || serviceName.isEmpty()) {
          throw new AmbariException("Could not find service for component"
              + ", componentName=" + request.getComponentName()
              + ", clusterName=" + cluster.getClusterName()
              + ", stackInfo=" + stackId.getStackId());
        }
        request.setServiceName(serviceName);
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
        duplicates.add("[clusterName=" + request.getClusterName() + ", hostName=" + request.getHostname() +
            ", componentName=" +request.getComponentName() +']');
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
          throw new IllegalArgumentException("Invalid desired state"
              + " only INIT state allowed during creation"
              + ", providedDesiredState=" + request.getDesiredState());
        }
      }

      Service s;
      try {
        s = cluster.getService(request.getServiceName());
      } catch (ServiceNotFoundException e) {
        throw new IllegalArgumentException(
            "The service[" + request.getServiceName() + "] associated with the component[" +
            request.getComponentName() + "] doesn't exist for the cluster[" + request.getClusterName() + "]");
      }
      ServiceComponent sc = s.getServiceComponent(
          request.getComponentName());

      Host host;
      try {
        host = clusters.getHost(request.getHostname());
      } catch (HostNotFoundException e) {
        throw new ParentObjectNotFoundException(
            "Attempted to add a host_component to a host that doesn't exist: ", e);
      }
      Set<Cluster> mappedClusters =
          clusters.getClustersForHost(request.getHostname());
      boolean validCluster = false;
      if (LOG.isDebugEnabled()) {
        LOG.debug("Looking to match host to cluster"
            + ", hostnameViaReg=" + host.getHostName()
            + ", hostname=" + request.getHostname()
            + ", clusterName=" + request.getClusterName()
            + ", hostClusterMapCount=" + mappedClusters.size());
      }
      for (Cluster mappedCluster : mappedClusters) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Host belongs to cluster"
              + ", hostname=" + request.getHostname()
              + ", clusterName=" + mappedCluster.getClusterName());
        }
        if (mappedCluster.getClusterName().equals(
            request.getClusterName())) {
          validCluster = true;
          break;
        }
      }
      if (!validCluster) {
        throw new ParentObjectNotFoundException("Attempted to add a host_component to a host that doesn't exist: " +
            "clusterName=" + request.getClusterName() + ", hostName=" + request.getHostname());
      }
      try {
        ServiceComponentHost sch = sc.getServiceComponentHost(
            request.getHostname());
        if (sch != null) {
          duplicates.add("[clusterName=" + request.getClusterName() + ", hostName=" + request.getHostname() +
              ", componentName=" +request.getComponentName() +']');
          continue;
        }
      } catch (AmbariException e) {
        // Expected
      }
    }

    // ensure only a single cluster update
    if (hostComponentNames.size() != 1) {
      throw new IllegalArgumentException("Invalid arguments - updates allowed"
          + " on only one cluster at a time");
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
      String msg;
      if (duplicates.size() == 1) {
        msg = "Attempted to create a host_component which already exists: ";
      } else {
        msg = "Attempted to create host_component's which already exist: ";
      }
      throw new DuplicateResourceException(msg + names.toString());
    }

    // now doing actual work
    for (ServiceComponentHostRequest request : requests) {
      Cluster cluster = clusters.getCluster(request.getClusterName());
      Service s = cluster.getService(request.getServiceName());
      ServiceComponent sc = s.getServiceComponent(
          request.getComponentName());

      StackId stackId = sc.getDesiredStackVersion();
      ComponentInfo compInfo = ambariMetaInfo.getComponentCategory(
          stackId.getStackName(), stackId.getStackVersion(),
          s.getName(), sc.getName());
      boolean isClient = compInfo.isClient();

      ServiceComponentHost sch =
          serviceComponentHostFactory.createNew(sc, request.getHostname(),
              isClient);

      if (request.getDesiredState() != null
          && !request.getDesiredState().isEmpty()) {
        State state = State.valueOf(request.getDesiredState());
        sch.setDesiredState(state);
      } 

      sch.setDesiredStackVersion(sc.getDesiredStackVersion());

      // TODO fix config versions to configs conversion
      Map<String, Config> configs = new HashMap<String, Config>();
      if (request.getConfigVersions() != null) {
      }

      sch.updateDesiredConfigs(configs);
      sc.addServiceComponentHost(sch);
      sch.persist();
    }

  }

  public synchronized void createConfiguration(
      ConfigurationRequest request) throws AmbariException {
    if (null == request.getClusterName() || request.getClusterName().isEmpty()
        || null == request.getType() || request.getType().isEmpty()
        || null == request.getVersionTag() || request.getVersionTag().isEmpty()
        || null == request.getConfigs() || request.getConfigs().isEmpty()) {
      throw new IllegalArgumentException("Invalid Arguments,"
          + " clustername, config type, config version and configs should not"
          + " be null or empty");
    }

    Cluster cluster = clusters.getCluster(request.getClusterName());

    Map<String, Config> configs = cluster.getDesiredConfigsByType(
        request.getType());
    if (null == configs) {
      configs = new HashMap<String, Config>();
    }

    Config config = configs.get(request.getVersionTag());
    if (configs.containsKey(request.getVersionTag())) {
      throw new AmbariException("Configuration with that tag exists for '"
          + request.getType() + "'");
    }

    config = configFactory.createNew (cluster, request.getType(),
        request.getConfigs());
    config.setVersionTag(request.getVersionTag());

    config.persist();

    cluster.addDesiredConfig(config);
  }

  @Override
  public void createUsers(Set<UserRequest> requests) throws AmbariException {

    for (UserRequest request : requests) {

      if (null == request.getUsername() || request.getUsername().isEmpty() ||
          null == request.getPassword() || request.getPassword().isEmpty()) {
        throw new AmbariException("Username and password must be supplied.");
      }

      User user = users.getAnyUser(request.getUsername());
      if (null != user)
        throw new AmbariException("User already exists.");

      users.createUser(request.getUsername(), request.getPassword());

      if (0 != request.getRoles().size()) {
        user = users.getAnyUser(request.getUsername());
        if (null != user) {
          for (String role : request.getRoles()) {
            if (!user.getRoles().contains(role))
              users.addRoleToUser(user, role);
          }
        }
      }
    }

  }

  private Stage createNewStage(Cluster cluster, long requestId) {
    String logDir = baseLogDir + "/" + requestId;
    Stage stage = new Stage(requestId, logDir, cluster.getClusterName());
    return stage;
  }

  private void createHostAction(Cluster cluster,
      Stage stage, ServiceComponentHost scHost,
      Map<String, Map<String, String>> configurations,
      RoleCommand command,
      long nowTimestamp,
      ServiceComponentHostEvent event) throws AmbariException {

    stage.addHostRoleExecutionCommand(scHost.getHostName(), Role.valueOf(scHost
        .getServiceComponentName()), command,
        event, scHost.getClusterName(),
        scHost.getServiceName());
    ExecutionCommand execCmd = stage.getExecutionCommandWrapper(scHost.getHostName(),
        scHost.getServiceComponentName()).getExecutionCommand();

    // Generate cluster host info
    execCmd.setClusterHostInfo(
        StageUtils.getClusterHostInfo(cluster));

    Host host = clusters.getHost(scHost.getHostName());

    execCmd.setConfigurations(configurations);

    // send stack info to agent
    StackId stackId = scHost.getDesiredStackVersion();
    Map<String, List<RepositoryInfo>> repos = ambariMetaInfo.getRepository(
        stackId.getStackName(), stackId.getStackVersion());
    String repoInfo = "";
    if (!repos.containsKey(host.getOsType())) {
      // FIXME should this be an error?
      LOG.warn("Could not retrieve repo information for host"
          + ", hostname=" + scHost.getHostName()
          + ", clusterName=" + cluster.getClusterName()
          + ", stackInfo=" + stackId.getStackId());
    } else {
      repoInfo = gson.toJson(repos.get(host.getOsType()));
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Sending repo information to agent"
          + ", hostname=" + scHost.getHostName()
          + ", clusterName=" + cluster.getClusterName()
          + ", stackInfo=" + stackId.getStackId()
          + ", repoInfo=" + repoInfo);
    }

    Map<String, String> params = new TreeMap<String, String>();
    params.put("repo_info", repoInfo);
    params.put("jdk_location", this.jdkResourceUrl);
    execCmd.setHostLevelParams(params);

    Map<String, String> roleParams = new TreeMap<String, String>();
    execCmd.setRoleParams(roleParams);

    return;
  }

  private synchronized Set<ClusterResponse> getClusters(ClusterRequest request)
      throws AmbariException {

    Set<ClusterResponse> response = new HashSet<ClusterResponse>();

    if (LOG.isDebugEnabled()) {
      LOG.debug("Received a getClusters request"
          + ", clusterName=" + request.getClusterName()
          + ", clusterId=" + request.getClusterId()
          + ", stackInfo=" + request.getStackVersion());
    }

    if (request.getClusterName() != null) {
      Cluster c = clusters.getCluster(request.getClusterName());
      response.add(c.convertToResponse());
      return response;
    } else if (request.getClusterId() != null) {
      Cluster c = clusters.getClusterById(request.getClusterId());
      response.add(c.convertToResponse());
      return response;
    }

    Map<String, Cluster> allClusters = clusters.getClusters();
    for (Cluster c : allClusters.values()) {
      if (request.getStackVersion() != null) {
        if (!request.getStackVersion().equals(
            c.getDesiredStackVersion().getStackId())) {
          // skip non matching stack versions
          continue;
        }
      }
      response.add(c.convertToResponse());
    }
    StringBuilder builder = new StringBuilder();
    if (LOG.isDebugEnabled()) {
      clusters.debugDump(builder);
      LOG.debug("Cluster State for cluster " + builder.toString());
    }
    return response;
  }

  private synchronized Set<ServiceResponse> getServices(ServiceRequest request)
      throws AmbariException {
    if (request.getClusterName() == null
        || request.getClusterName().isEmpty()) {
      throw new AmbariException("Invalid arguments, cluster name"
          + " cannot be null");
    }
    String clusterName = request.getClusterName();
    final Cluster cluster;
    try {
      cluster = clusters.getCluster(clusterName);
    } catch (ObjectNotFoundException e) {
      throw new ParentObjectNotFoundException("Parent Cluster resource doesn't exist", e);
    }

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
        throw new IllegalArgumentException("Invalid arguments, invalid desired"
            + " state, desiredState=" + desiredStateToCheck);
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

  private synchronized Set<ServiceComponentResponse> getComponents(
      ServiceComponentRequest request) throws AmbariException {
    if (request.getClusterName() == null
        || request.getClusterName().isEmpty()) {
      throw new IllegalArgumentException("Invalid arguments, cluster name"
          + " should be non-null");
    }

    final Cluster cluster;
    try {
      cluster = clusters.getCluster(request.getClusterName());
    } catch (ObjectNotFoundException e) {
      throw new ParentObjectNotFoundException("Parent Cluster resource doesn't exist", e);
    }

    Set<ServiceComponentResponse> response =
        new HashSet<ServiceComponentResponse>();

    if (request.getComponentName() != null) {
      if (request.getServiceName() == null) {
        StackId stackId = cluster.getDesiredStackVersion();
        String serviceName =
            ambariMetaInfo.getComponentToService(stackId.getStackName(),
                stackId.getStackVersion(), request.getComponentName());
        if (LOG.isDebugEnabled()) {
          LOG.debug("Looking up service name for component"
              + ", componentName=" + request.getComponentName()
              + ", serviceName=" + serviceName);
        }
        if (serviceName == null
            || serviceName.isEmpty()) {
          throw new AmbariException("Could not find service for component"
              + ", componentName=" + request.getComponentName()
              + ", clusterName=" + cluster.getClusterName()
              + ", stackInfo=" + stackId.getStackId());
        }
        request.setServiceName(serviceName);
      }

      final Service s;
      try {
        s = cluster.getService(request.getServiceName());
      } catch (ObjectNotFoundException e) {
        throw new ParentObjectNotFoundException("Parent Service resource doesn't exist", e);
      }

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
        throw new IllegalArgumentException("Invalid arguments, invalid desired"
            + " state, desiredState=" + desiredStateToCheck);
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

  private synchronized Set<HostResponse> getHosts(HostRequest request)
      throws AmbariException {

    //TODO/FIXME host can only belong to a single cluster so get host directly from Cluster
    //TODO/FIXME what is the requirement for filtering on host attributes?

    List<Host>        hosts;
    Set<HostResponse> response = new HashSet<HostResponse>();
    Cluster           cluster  = null;

    String clusterName = request.getClusterName();
    String hostName    = request.getHostname();

    if (clusterName != null) {
      //validate that cluster exists, throws exception if it doesn't.
      try {
        cluster = clusters.getCluster(clusterName);
      } catch (ObjectNotFoundException e) {
        throw new ParentObjectNotFoundException("Parent Cluster resource doesn't exist", e);
      }
    }

    if (hostName == null) {
      hosts = clusters.getHosts();
    } else {
      hosts = new ArrayList<Host>();
      hosts.add(clusters.getHost(request.getHostname()));
    }

    for (Host h : hosts) {
      if (clusterName != null) {
        if (clusters.getClustersForHost(h.getHostName()).contains(cluster)) {
          HostResponse r = h.convertToResponse();
          r.setClusterName(clusterName);
          response.add(r);
        } else if (hostName != null) {
          throw new HostNotFoundException(hostName);
        }
      } else {
        HostResponse r = h.convertToResponse();

        Set<Cluster> clustersForHost = clusters.getClustersForHost(h.getHostName());
        //todo: host can only belong to a single cluster
        if (clustersForHost != null && clustersForHost.size() != 0) {
          r.setClusterName(clustersForHost.iterator().next().getClusterName());
        }
        response.add(r);
      }
    }
    return response;
  }

  private synchronized Set<ServiceComponentHostResponse> getHostComponents(
      ServiceComponentHostRequest request) throws AmbariException {
    if (request.getClusterName() == null
        || request.getClusterName().isEmpty()) {
      throw new IllegalArgumentException("Invalid arguments, cluster name should not be null");
    }

    final Cluster cluster;
    try {
      cluster = clusters.getCluster(request.getClusterName());
    } catch (ClusterNotFoundException e) {
      throw new ParentObjectNotFoundException("Parent Cluster resource doesn't exist", e);
    }

    if (request.getHostname() != null) {
      try {
        if (! clusters.getClustersForHost(request.getHostname()).contains(cluster)) {
          // case where host exists but not associated with given cluster
          throw new ParentObjectNotFoundException("Parent Host resource doesn't exist",
              new HostNotFoundException(request.getClusterName(), request.getHostname()));
        }
      } catch (HostNotFoundException e) {
        // creating new HostNotFoundException to add cluster name
        throw new ParentObjectNotFoundException("Parent Host resource doesn't exist",
            new HostNotFoundException(request.getClusterName(), request.getHostname()));
      }
    }

    if (request.getComponentName() != null) {
      if (request.getServiceName() == null
          || request.getServiceName().isEmpty()) {
        StackId stackId = cluster.getDesiredStackVersion();
        String serviceName =
            ambariMetaInfo.getComponentToService(stackId.getStackName(),
                stackId.getStackVersion(), request.getComponentName());
        if (LOG.isDebugEnabled()) {
          LOG.debug("Looking up service name for component"
              + ", componentName=" + request.getComponentName()
              + ", serviceName=" + serviceName
              + ", stackInfo=" + stackId.getStackId());
        }
        if (serviceName == null
            || serviceName.isEmpty()) {
          throw new ServiceComponentHostNotFoundException(
              cluster.getClusterName(), null, request.getComponentName(),request.getHostname());
        }
        request.setServiceName(serviceName);
      }
    }

    Set<Service> services = new HashSet<Service>();
    if (request.getServiceName() != null && !request.getServiceName().isEmpty()) {
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
        throw new IllegalArgumentException("Invalid arguments, invalid desired"
            + " state, desiredState=" + desiredStateToCheck);
      }
      checkDesiredState = true;
    }

    for (Service s : services) {
      // filter on component name if provided
      Set<ServiceComponent> components = new HashSet<ServiceComponent>();
      if (request.getComponentName() != null) {
        components.add(s.getServiceComponent(request.getComponentName()));
      } else {
        components.addAll(s.getServiceComponents().values());
      }
      for(ServiceComponent sc : components) {
        if (request.getComponentName() != null) {
          if (!sc.getName().equals(request.getComponentName())) {
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
            if (request.getServiceName() != null && request.getComponentName() != null) {
              throw new ServiceComponentHostNotFoundException(cluster.getClusterName(),
                  request.getServiceName(), request.getComponentName(),request.getHostname());
            } else {
              // ignore this since host_component was not specified
              // this is an artifact of how we get host_components and can happen
              // in case where we get all host_components for a host
            }

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


  private synchronized Set<ConfigurationResponse> getConfigurations(
      ConfigurationRequest request) throws AmbariException {
    if (request.getClusterName() == null) {
      throw new IllegalArgumentException("Invalid arguments, cluster name"
          + " should not be null");
    }

    Cluster cluster = clusters.getCluster(request.getClusterName());

    Set<ConfigurationResponse> responses = new HashSet<ConfigurationResponse>();

    // !!! if only one, then we need full properties
    if (null != request.getType() && null != request.getVersionTag()) {
      Config config = cluster.getDesiredConfig(request.getType(),
          request.getVersionTag());
      if (null != config) {
        ConfigurationResponse response = new ConfigurationResponse(
            cluster.getClusterName(), config.getType(), config.getVersionTag(),
            config.getProperties());
        responses.add(response);
      }
    }
    else {
      if (null != request.getType()) {
        Map<String, Config> configs = cluster.getDesiredConfigsByType(
            request.getType());

        if (null != configs) {
          for (Entry<String, Config> entry : configs.entrySet()) {
            ConfigurationResponse response = new ConfigurationResponse(
                cluster.getClusterName(), request.getType(),
                entry.getValue().getVersionTag(), new HashMap<String, String>());
            responses.add(response);
          }
        }
      } else {
        // !!! all configuration
        Collection<Config> all = cluster.getAllConfigs();

        for (Config config : all) {
          ConfigurationResponse response = new ConfigurationResponse(
             cluster.getClusterName(), config.getType(), config.getVersionTag(),
             new HashMap<String, String>());

          responses.add(response);
        }
      }
    }

    return responses;

  }


  @Override
  public synchronized RequestStatusResponse updateCluster(ClusterRequest request)
      throws AmbariException {
    // for now only update host list supported
    if (request.getClusterName() == null
        || request.getClusterName().isEmpty()) {
      throw new IllegalArgumentException("Invalid arguments, cluster name"
          + " should not be null");
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Received a updateCluster request"
          + ", clusterName=" + request.getClusterName()
          + ", request=" + request);
    }

    final Cluster c = clusters.getCluster(request.getClusterName());
    clusters.mapHostsToCluster(request.getHostNames(),
        request.getClusterName());

    if (!request.getStackVersion().equals(
        c.getDesiredStackVersion().getStackId())) {
      throw new IllegalArgumentException("Update of desired stack version"
          + " not supported");
    }

    return null;
  }

  // FIXME refactor code out of all update functions
  /*
  private TrackActionResponse triggerStateChange(State newState, Service s,
      ServiceComponent sc, ServiceComponentHost sch) {
    return null;
  }
  */
  
  private String getJobTrackerHost(Cluster cluster) {
    try {
      Service svc = cluster.getService("MAPREDUCE");
      ServiceComponent sc = svc.getServiceComponent(Role.JOBTRACKER.toString());
      if (sc.getServiceComponentHosts() != null
          && !sc.getServiceComponentHosts().isEmpty()) {
        return sc.getServiceComponentHosts().keySet().iterator().next();
      }
    } catch (AmbariException ex) {
      return null;
    }
    return null;
  }

  private RequestStatusResponse doStageCreation(Cluster cluster,
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

    if ((changedServices == null || changedServices.isEmpty())
        && (changedComps == null || changedComps.isEmpty())
        && (changedScHosts == null || changedScHosts.isEmpty())) {
      return null;
    }

    Long requestId = null;
    List<Stage> stages = null;

    Set<String> smokeTestServices =
        new HashSet<String>();

    // smoke test any service that goes from installed to started
    if (changedServices != null) {
      for (Entry<State, List<Service>> entry : changedServices.entrySet()) {
        if (State.STARTED != entry.getKey()) {
          continue;
        }
        for (Service s : entry.getValue()) {
          if (State.INSTALLED == s.getDesiredState()) {
            smokeTestServices.add(s.getName());
          }
        }
      }
    }

    Map<String, Map<String, Integer>> changedComponentCount =
        new HashMap<String, Map<String, Integer>>();
    for (Map<State, List<ServiceComponentHost>> stateScHostMap :
      changedScHosts.values()) {
      for (Entry<State, List<ServiceComponentHost>> entry :
          stateScHostMap.entrySet()) {
        if (State.STARTED != entry.getKey()) {
          continue;
        }
        for (ServiceComponentHost sch : entry.getValue()) {
          if (State.START_FAILED != sch.getState()
              && State.INSTALLED != sch.getState()) {
            continue;
          }
          if (!changedComponentCount.containsKey(sch.getServiceName())) {
            changedComponentCount.put(sch.getServiceName(),
                new HashMap<String, Integer>());
          }
          if (!changedComponentCount.get(sch.getServiceName())
              .containsKey(sch.getServiceComponentName())) {
            changedComponentCount.get(sch.getServiceName())
                .put(sch.getServiceComponentName(), 1);
          } else {
            Integer i = changedComponentCount.get(sch.getServiceName())
                .get(sch.getServiceComponentName());
            changedComponentCount.get(sch.getServiceName())
              .put(sch.getServiceComponentName(), ++i);
          }
        }
      }
    }

    for (Entry<String, Map<String, Integer>> entry :
        changedComponentCount.entrySet()) {
      String serviceName = entry.getKey();
      // smoke test service if more than one component is started
      if (entry.getValue().size() > 1) {
        smokeTestServices.add(serviceName);
        continue;
      }
      for (String componentName :
        changedComponentCount.get(serviceName).keySet()) {
        ServiceComponent sc = cluster.getService(serviceName)
            .getServiceComponent(componentName);
        StackId stackId = sc.getDesiredStackVersion();
        ComponentInfo compInfo = ambariMetaInfo.getComponentCategory(
            stackId.getStackName(), stackId.getStackVersion(), serviceName,
            componentName);
        if (compInfo.isMaster()) {
          smokeTestServices.add(serviceName);
        }

        // FIXME if master check if we need to run a smoke test for the master
      }
    }

    if (!changedScHosts.isEmpty()
        || !smokeTestServices.isEmpty()) {
      long nowTimestamp = System.currentTimeMillis();
      requestId = Long.valueOf(actionManager.getNextRequestId());

      // FIXME cannot work with a single stage
      // multiple stages may be needed for reconfigure
      long stageId = 0;
      Stage stage = createNewStage(cluster, requestId.longValue());
      stage.setStageId(stageId);
      //HACK
      String jobtrackerHost = this.getJobTrackerHost(cluster);
      for (String compName : changedScHosts.keySet()) {
        for (State newState : changedScHosts.get(compName).keySet()) {
          for (ServiceComponentHost scHost :
              changedScHosts.get(compName).get(newState)) {
            RoleCommand roleCommand;
            State oldSchState = scHost.getState();
            ServiceComponentHostEvent event;
            switch(newState) {
              case INSTALLED:
                if (oldSchState == State.INIT
                    || oldSchState == State.UNINSTALLED
                    || oldSchState == State.INSTALLED
                    || oldSchState == State.INSTALLING
                    || oldSchState == State.INSTALL_FAILED) {
                  roleCommand = RoleCommand.INSTALL;
                  event = new ServiceComponentHostInstallEvent(
                      scHost.getServiceComponentName(), scHost.getHostName(),
                      nowTimestamp,
                      scHost.getDesiredStackVersion().getStackId());
                } else if (oldSchState == State.STARTED
                    || oldSchState == State.START_FAILED
                    || oldSchState == State.INSTALLED
                    || oldSchState == State.STOP_FAILED) {
                  roleCommand = RoleCommand.STOP;
                  event = new ServiceComponentHostStopEvent(
                      scHost.getServiceComponentName(), scHost.getHostName(),
                      nowTimestamp);
                } else {
                  throw new AmbariException("Invalid transition for"
                      + " servicecomponenthost"
                      + ", clusterName=" + cluster.getClusterName()
                      + ", clusterId=" + cluster.getClusterId()
                      + ", serviceName=" + scHost.getServiceName()
                      + ", componentName=" + scHost.getServiceComponentName()
                      + ", hostname=" + scHost.getHostName()
                      + ", currentState=" + oldSchState
                      + ", newDesiredState=" + newState);
                }
                break;
              case STARTED:
                StackId stackId = scHost.getDesiredStackVersion();
                ComponentInfo compInfo = ambariMetaInfo.getComponentCategory(
                    stackId.getStackName(), stackId.getStackVersion(), scHost.getServiceName(),
                    scHost.getServiceComponentName());
                if (oldSchState == State.INSTALLED
                    || oldSchState == State.START_FAILED || oldSchState == State.STARTING) {
                  roleCommand = RoleCommand.START;
                  event = new ServiceComponentHostStartEvent(
                      scHost.getServiceComponentName(), scHost.getHostName(),
                      nowTimestamp, scHost.getDesiredConfigVersionsRecursive());
                } else {
                  String error = "Invalid transition for"
                      + " servicecomponenthost"
                      + ", clusterName=" + cluster.getClusterName()
                      + ", clusterId=" + cluster.getClusterId()
                      + ", serviceName=" + scHost.getServiceName()
                      + ", componentName=" + scHost.getServiceComponentName()
                      + ", hostname=" + scHost.getHostName()
                      + ", currentState=" + oldSchState
                      + ", newDesiredState=" + newState;
                  if (compInfo.isMaster()) {
                    throw new AmbariException(error);
                  } else {
                    LOG.info("Ignoring: " + error);
                    continue;
                  }
                }
                break;
              case UNINSTALLED:
                if (oldSchState == State.INSTALLED
                    || oldSchState == State.UNINSTALL_FAILED) {
                  roleCommand = RoleCommand.UNINSTALL;
                  event = new ServiceComponentHostStartEvent(
                      scHost.getServiceComponentName(), scHost.getHostName(),
                      nowTimestamp, scHost.getDesiredConfigVersionsRecursive());
                } else {
                  throw new AmbariException("Invalid transition for"
                      + " servicecomponenthost"
                      + ", clusterName=" + cluster.getClusterName()
                      + ", clusterId=" + cluster.getClusterId()
                      + ", serviceName=" + scHost.getServiceName()
                      + ", componentName=" + scHost.getServiceComponentName()
                      + ", hostname=" + scHost.getHostName()
                      + ", currentState=" + oldSchState
                      + ", newDesiredState=" + newState);
                }
                break;
              case INIT:
                throw new AmbariException("Unsupported transition to INIT for"
                    + " servicecomponenthost"
                    + ", clusterName=" + cluster.getClusterName()
                    + ", clusterId=" + cluster.getClusterId()
                    + ", serviceName=" + scHost.getServiceName()
                    + ", componentName=" + scHost.getServiceComponentName()
                    + ", hostname=" + scHost.getHostName()
                    + ", currentState=" + oldSchState
                    + ", newDesiredState=" + newState);
              default:
                throw new AmbariException("Unsupported state change operation"
                    + ", newState=" + newState.toString());
            }

            if (LOG.isDebugEnabled()) {
              LOG.debug("Create a new host action"
                  + ", requestId=" + requestId.longValue()
                  + ", componentName=" + scHost.getServiceComponentName()
                  + ", hostname=" + scHost.getHostName()
                  + ", roleCommand=" + roleCommand.name());
            }

            Map<String, Config> configs = scHost.getDesiredConfigs();
            // Clone configurations for the command
            Map<String, Map<String, String>> configurations =
                new TreeMap<String, Map<String, String>>();
            for (Config config : configs.values()) {
              if (LOG.isDebugEnabled()) {
                LOG.debug("Cloning configs for execution command"
                    + ", configType=" + config.getType()
                    + ", configVersionTag=" + config.getVersionTag()
                    + ", clusterName=" + scHost.getClusterName()
                    + ", serviceName=" + scHost.getServiceName()
                    + ", componentName=" + scHost.getServiceComponentName()
                    + ", hostname=" + scHost.getHostName());
              }
              configurations.put(config.getType(),
                  config.getProperties());
            }
            // HACK HACK HACK
            if ((!scHost.getHostName().equals(jobtrackerHost))
                && configurations.get("global") != null) {
              if (LOG.isDebugEnabled()) {
                LOG.debug("Setting rca_enabled to false for host "
                    + scHost.getHostName());
              }
              configurations.get("global").put("rca_enabled", "false");
            }
            createHostAction(cluster, stage, scHost, configurations,
                roleCommand, nowTimestamp, event);
          }
        }
      }

      for (String serviceName : smokeTestServices) {
        Service s = cluster.getService(serviceName);

        // find service component host
        String clientHost = getClientHostForRunningAction(cluster, s);
        String smokeTestRole =
            actionMetadata.getServiceCheckAction(serviceName);

        if (clientHost == null || smokeTestRole == null) {
          LOG.info("Nothing to do for service check as could not find role or"
              + " or host to run check on"
              + ", clusterName=" + cluster.getClusterName()
              + ", serviceName=" + serviceName
              + ", clientHost=" + clientHost
              + ", serviceCheckRole=" + smokeTestRole);
          continue;
        }

        stage.addHostRoleExecutionCommand(clientHost,
            Role.valueOf(smokeTestRole),
            RoleCommand.EXECUTE,
            new ServiceComponentHostOpInProgressEvent(null, clientHost,
                nowTimestamp), cluster.getClusterName(), serviceName);

        Map<String, Map<String, String>> configurations =
            new TreeMap<String, Map<String, String>>();
        Map<String, Config> allConfigs = cluster.getService(serviceName).getDesiredConfigs();
        if (allConfigs != null) {
          for (Map.Entry<String, Config> entry: allConfigs.entrySet()) {
            configurations.put(entry.getValue().getType(), entry.getValue().getProperties());
          }
        }
        
        stage.getExecutionCommandWrapper(clientHost,
            smokeTestRole).getExecutionCommand()
            .setConfigurations(configurations);

        // Generate cluster host info
        stage.getExecutionCommandWrapper(clientHost, smokeTestRole)
            .getExecutionCommand()
            .setClusterHostInfo(StageUtils.getClusterHostInfo(cluster));
      }

      RoleGraph rg = new RoleGraph(rco);
      rg.build(stage);
      stages = rg.getStages();

      if (LOG.isDebugEnabled()) {
        LOG.debug("Triggering Action Manager"
            + ", clusterName=" + cluster.getClusterName()
            + ", requestId=" + requestId.longValue()
            + ", stagesCount=" + stages.size());
      }
      actionManager.sendActions(stages);
    }

    if (changedServices != null) {
      for (Entry<State, List<Service>> entry : changedServices.entrySet()) {
        State newState = entry.getKey();
        for (Service s : entry.getValue()) {
          if (s.isClientOnlyService()
              && newState == State.STARTED) {
            continue;
          }
          s.setDesiredState(newState);
        }
      }
    }

    if (changedComps != null) {
      for (Entry<State, List<ServiceComponent>> entry :
          changedComps.entrySet()){
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

    if (stages == null || stages.isEmpty()
        || requestId == null) {
      return null;
    }
    return getRequestStatusResponse(requestId.longValue());
  }

  private boolean isValidStateTransition(State oldState,
      State newState) {
    switch(newState) {
      case INSTALLED:
        if (oldState == State.INIT
            || oldState == State.UNINSTALLED
            || oldState == State.INSTALLED
            || oldState == State.INSTALLING
            || oldState == State.STARTED
            || oldState == State.START_FAILED
            || oldState == State.INSTALL_FAILED
            || oldState == State.STOP_FAILED) {
          return true;
        }
        break;
      case STARTED:
        if (oldState == State.INSTALLED
            || oldState == State.STARTING
            || oldState == State.STARTED
            || oldState == State.START_FAILED) {
          return true;
        }
        break;
      case UNINSTALLED:
        if (oldState == State.INSTALLED
            || oldState == State.UNINSTALLED
            || oldState == State.UNINSTALL_FAILED) {
          return true;
        }
      case INIT:
        if (oldState == State.UNINSTALLED
            || oldState == State.INIT
            || oldState == State.WIPEOUT_FAILED) {
          return true;
        }
    }
    return false;
  }


  private boolean isValidDesiredStateTransition(State oldState,
      State newState) {
    switch(newState) {
      case INSTALLED:
        if (oldState == State.INIT
            || oldState == State.UNINSTALLED
            || oldState == State.INSTALLED
            || oldState == State.STARTED) {
          return true;
        }
        break;
      case STARTED:
        if (oldState == State.INSTALLED
            || oldState == State.STARTED) {
          return true;
        }
        break;
    }
    return false;
  }

  private void safeToUpdateConfigsForServiceComponentHost(
      ServiceComponentHost sch,
      State currentState, State newDesiredState)
          throws AmbariException {
    if (currentState == State.STARTED
        || currentState == State.STARTING) {
      throw new AmbariException("Changing of configs not supported"
          + " in STARTING or STARTED state"
          + ", clusterName=" + sch.getClusterName()
          + ", serviceName=" + sch.getServiceName()
          + ", componentName=" + sch.getServiceComponentName()
          + ", hostname=" + sch.getHostName()
          + ", currentState=" + currentState
          + ", newDesiredState=" + newDesiredState);
    }

    if (newDesiredState != null) {
      if (!(newDesiredState == State.INIT
          || newDesiredState == State.INSTALLED
          || newDesiredState == State.STARTED)) {
        throw new AmbariException("Changing of configs not supported"
            + " for this transition"
            + ", clusterName=" + sch.getClusterName()
            + ", serviceName=" + sch.getServiceName()
            + ", componentName=" + sch.getServiceComponentName()
            + ", hostname=" + sch.getHostName()
            + ", currentState=" + currentState
            + ", newDesiredState=" + newDesiredState);
      }
    }
  }

  private void safeToUpdateConfigsForServiceComponent(
      ServiceComponent sc,
      State currentDesiredState, State newDesiredState)
          throws AmbariException {
    for (ServiceComponentHost sch :
      sc.getServiceComponentHosts().values()) {
      safeToUpdateConfigsForServiceComponentHost(sch,
        sch.getState(), newDesiredState);
    }
  }

  private void safeToUpdateConfigsForService(Service service,
      State currentDesiredState, State newDesiredState)
          throws AmbariException {
    for (ServiceComponent component :
        service.getServiceComponents().values()) {
      safeToUpdateConfigsForServiceComponent(component,
          component.getDesiredState(), newDesiredState);
    }
  }

  @Override
  public synchronized RequestStatusResponse updateServices(
      Set<ServiceRequest> requests) throws AmbariException {

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
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
        throw new IllegalArgumentException("Invalid arguments, cluster name"
            + " and service name should be provided to update services");
      }

      if (LOG.isDebugEnabled()) {
        LOG.debug("Received a updateService request"
            + ", clusterName=" + request.getClusterName()
            + ", serviceName=" + request.getServiceName()
            + ", request=" + request.toString());
      }

      clusterNames.add(request.getClusterName());

      if (clusterNames.size() > 1) {
        throw new IllegalArgumentException("Updates to multiple clusters is not"
            + " supported");
      }

      if (!serviceNames.containsKey(request.getClusterName())) {
        serviceNames.put(request.getClusterName(), new HashSet<String>());
      }
      if (serviceNames.get(request.getClusterName())
          .contains(request.getServiceName())) {
        // TODO throw single exception
        throw new IllegalArgumentException("Invalid request contains duplicate"
            + " service names");
      }
      serviceNames.get(request.getClusterName()).add(request.getServiceName());

      Cluster cluster = clusters.getCluster(request.getClusterName());
      Service s = cluster.getService(request.getServiceName());
      State oldState = s.getDesiredState();
      State newState = null;
      if (request.getDesiredState() != null) {
        newState = State.valueOf(request.getDesiredState());
        if (!newState.isValidDesiredState()) {
          throw new IllegalArgumentException("Invalid arguments, invalid"
              + " desired state, desiredState=" + newState);
        }
      }

      if (request.getConfigVersions() != null) {
        safeToUpdateConfigsForService(s, oldState, newState);

        for (Entry<String,String> entry :
            request.getConfigVersions().entrySet()) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Attaching config to service"
                + ", clusterName=" + cluster.getClusterName()
                + ", serviceName=" + s.getName()
                + ", configType=" + entry.getKey()
                + ", configTag=" + entry.getValue());
          }
          Config config = cluster.getDesiredConfig(
              entry.getKey(), entry.getValue());
          if (null == config) {
            // throw error for invalid config
            throw new AmbariException("Trying to update service with"
                + " invalid configs"
                + ", clusterName=" + cluster.getClusterName()
                + ", clusterId=" + cluster.getClusterId()
                + ", serviceName=" + s.getName()
                + ", invalidConfigType=" + entry.getKey()
                + ", invalidConfigTag=" + entry.getValue());
          }
        }
      }


      if (newState == null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Nothing to do for new updateService request"
              + ", clusterName=" + request.getClusterName()
              + ", serviceName=" + request.getServiceName()
              + ", newDesiredState=null");
        }
        continue;
      }

      seenNewStates.add(newState);

      if (newState != oldState) {
        if (!isValidDesiredStateTransition(oldState, newState)) {
          throw new AmbariException("Invalid transition for"
              + " service"
              + ", clusterName=" + cluster.getClusterName()
              + ", clusterId=" + cluster.getClusterId()
              + ", serviceName=" + s.getName()
              + ", currentDesiredState=" + oldState
              + ", newDesiredState=" + newState);

        }
        if (!changedServices.containsKey(newState)) {
          changedServices.put(newState, new ArrayList<Service>());
        }
        changedServices.get(newState).add(s);
      }

      // TODO should we check whether all servicecomponents and
      // servicecomponenthosts are in the required desired state?

      for (ServiceComponent sc : s.getServiceComponents().values()) {
        State oldScState = sc.getDesiredState();
        if (newState != oldScState) {
          if (sc.isClientComponent() &&
              !newState.isValidClientComponentState()) {
            continue;
          }
          if (!isValidDesiredStateTransition(oldScState, newState)) {
            throw new AmbariException("Invalid transition for"
                + " servicecomponent"
                + ", clusterName=" + cluster.getClusterName()
                + ", clusterId=" + cluster.getClusterId()
                + ", serviceName=" + sc.getServiceName()
                + ", componentName=" + sc.getName()
                + ", currentDesiredState=" + oldScState
                + ", newDesiredState=" + newState);
          }
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
              + ", currentDesiredState=" + oldScState
              + ", newDesiredState=" + newState);
        }
        for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()){
          State oldSchState = sch.getState();
          if (newState == oldSchState) {
            sch.setDesiredState(newState);
            if (LOG.isDebugEnabled()) {
              LOG.debug("Ignoring ServiceComponentHost"
                  + ", clusterName=" + request.getClusterName()
                  + ", serviceName=" + s.getName()
                  + ", componentName=" + sc.getName()
                  + ", hostname=" + sch.getHostName()
                  + ", currentState=" + oldSchState
                  + ", newDesiredState=" + newState);
            }
            continue;
          }
          if (sc.isClientComponent() &&
              !newState.isValidClientComponentState()) {
            continue;
          }
          /** 
           * This is hack for now wherein we dont fail if the 
           * sch is in INSTALL_FAILED 
           */
          if (!isValidStateTransition(oldSchState, newState)) {
            String error = "Invalid transition for"
                + " servicecomponenthost"
                + ", clusterName=" + cluster.getClusterName()
                + ", clusterId=" + cluster.getClusterId()
                + ", serviceName=" + sch.getServiceName()
                + ", componentName=" + sch.getServiceComponentName()
                + ", hostname=" + sch.getHostName()
                + ", currentState=" + oldSchState
                + ", newDesiredState=" + newState;
            StackId sid = cluster.getDesiredStackVersion();
            
            if ( ambariMetaInfo.getComponentCategory(
                sid.getStackName(), sid.getStackVersion(), sc.getServiceName(),
                sch.getServiceComponentName()).isMaster()) {
              throw new AmbariException(error);
            } else {
              LOG.warn("Ignoring: " + error);
              continue;
            }
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
                + ", currentState=" + oldSchState
                + ", newDesiredState=" + newState);
          }
          changedScHosts.get(sc.getName()).get(newState).add(sch);
        }
      }
    }

    if (seenNewStates.size() > 1) {
      // TODO should we handle this scenario
      throw new IllegalArgumentException("Cannot handle different desired state"
          + " changes for a set of services at the same time");
    }

    for (ServiceRequest request : requests) {
      Cluster cluster = clusters.getCluster(request.getClusterName());
      Service s = cluster.getService(request.getServiceName());
      if (request.getConfigVersions() != null) {
        Map<String, Config> updated = new HashMap<String, Config>();

        for (Entry<String,String> entry : request.getConfigVersions().entrySet()) {
          Config config = cluster.getDesiredConfig(entry.getKey(), entry.getValue());
          updated.put(config.getType(), config);
        }

        if (!updated.isEmpty()) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Updating service configs, attaching configs"
                + ", clusterName=" + request.getClusterName()
                + ", serviceName=" + s.getName()
                + ", configCount=" + updated.size());
          }
          s.updateDesiredConfigs(updated);
          s.persist();
        }

        for (ServiceComponent sc : s.getServiceComponents().values()) {
          sc.deleteDesiredConfigs(updated.keySet());
          for (ServiceComponentHost sch :
            sc.getServiceComponentHosts().values()) {
            sch.deleteDesiredConfigs(updated.keySet());
            sch.persist();
          }
          sc.persist();
        }
      }
    }

    Cluster cluster = clusters.getCluster(clusterNames.iterator().next());

    return doStageCreation(cluster, changedServices,
        changedComps, changedScHosts);
  }

  @Override
  public synchronized RequestStatusResponse updateComponents(
      Set<ServiceComponentRequest> requests) throws AmbariException {

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
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
          || request.getComponentName() == null
          || request.getComponentName().isEmpty()) {
        throw new IllegalArgumentException("Invalid arguments, cluster name"
            + ", service name and component name should be provided to"
            + " update components");
      }

      Cluster cluster = clusters.getCluster(request.getClusterName());

      if (request.getServiceName() == null
          || request.getServiceName().isEmpty()) {
        StackId stackId = cluster.getDesiredStackVersion();
        String serviceName =
            ambariMetaInfo.getComponentToService(stackId.getStackName(),
                stackId.getStackVersion(), request.getComponentName());
        if (LOG.isDebugEnabled()) {
          LOG.debug("Looking up service name for component"
              + ", componentName=" + request.getComponentName()
              + ", serviceName=" + serviceName);
        }

        if (serviceName == null
            || serviceName.isEmpty()) {
          throw new AmbariException("Could not find service for component"
              + ", componentName=" + request.getComponentName()
              + ", clusterName=" + cluster.getClusterName()
              + ", stackInfo=" + stackId.getStackId());
        }
        request.setServiceName(serviceName);
      }

      if (LOG.isDebugEnabled()) {
        LOG.debug("Received a updateComponent request"
            + ", clusterName=" + request.getClusterName()
            + ", serviceName=" + request.getServiceName()
            + ", componentName=" + request.getComponentName()
            + ", request=" + request);
      }

      clusterNames.add(request.getClusterName());

      if (clusterNames.size() > 1) {
        // FIXME throw correct error
        throw new IllegalArgumentException("Updates to multiple clusters is not"
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
        throw new IllegalArgumentException("Invalid request contains duplicate"
            + " service components");
      }
      componentNames.get(request.getClusterName())
          .get(request.getServiceName()).add(request.getComponentName());

      Service s = cluster.getService(request.getServiceName());
      ServiceComponent sc = s.getServiceComponent(
        request.getComponentName());
      State oldState = sc.getDesiredState();
      State newState = null;
      if (request.getDesiredState() != null) {
        newState = State.valueOf(request.getDesiredState());
        if (!newState.isValidDesiredState()) {
          throw new IllegalArgumentException("Invalid arguments, invalid"
              + " desired state, desiredState=" + newState.toString());
        }
      }

      if (request.getConfigVersions() != null) {
        safeToUpdateConfigsForServiceComponent(sc, oldState, newState);

        for (Entry<String,String> entry :
            request.getConfigVersions().entrySet()) {
          Config config = cluster.getDesiredConfig(
              entry.getKey(), entry.getValue());
          if (null == config) {
            // throw error for invalid config
            throw new AmbariException("Trying to update servicecomponent with"
                + " invalid configs"
                + ", clusterName=" + cluster.getClusterName()
                + ", clusterId=" + cluster.getClusterId()
                + ", serviceName=" + s.getName()
                + ", componentName=" + sc.getName()
                + ", invalidConfigType=" + entry.getKey()
                + ", invalidConfigTag=" + entry.getValue());
          }
        }
      }

      if (newState == null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Nothing to do for new updateServiceComponent request"
              + ", clusterName=" + request.getClusterName()
              + ", serviceName=" + request.getServiceName()
              + ", componentName=" + request.getComponentName()
              + ", newDesiredState=null");
        }
        continue;
      }

      if (sc.isClientComponent() &&
          !newState.isValidClientComponentState()) {
        throw new AmbariException("Invalid desired state for a client"
            + " component");
      }

      seenNewStates.add(newState);

      State oldScState = sc.getDesiredState();
      if (newState != oldScState) {
        if (!isValidDesiredStateTransition(oldScState, newState)) {
          // FIXME throw correct error
          throw new AmbariException("Invalid transition for"
              + " servicecomponent"
              + ", clusterName=" + cluster.getClusterName()
              + ", clusterId=" + cluster.getClusterId()
              + ", serviceName=" + sc.getServiceName()
              + ", componentName=" + sc.getName()
              + ", currentDesiredState=" + oldScState
              + ", newDesiredState=" + newState);
        }
        if (!changedComps.containsKey(newState)) {
          changedComps.put(newState, new ArrayList<ServiceComponent>());
        }
        if (LOG.isDebugEnabled()) {
          LOG.debug("Handling update to ServiceComponent"
              + ", clusterName=" + request.getClusterName()
              + ", serviceName=" + s.getName()
              + ", componentName=" + sc.getName()
              + ", currentDesiredState=" + oldScState
              + ", newDesiredState=" + newState);
        }
        changedComps.get(newState).add(sc);
      }

      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        State oldSchState = sch.getState();
        if (newState == oldSchState) {
          sch.setDesiredState(newState);
          if (LOG.isDebugEnabled()) {
            LOG.debug("Ignoring ServiceComponentHost"
                + ", clusterName=" + request.getClusterName()
                + ", serviceName=" + s.getName()
                + ", componentName=" + sc.getName()
                + ", hostname=" + sch.getHostName()
                + ", currentState=" + oldSchState
                + ", newDesiredState=" + newState);
          }
          continue;
        }
        if (!isValidStateTransition(oldSchState, newState)) {
          // FIXME throw correct error
          throw new AmbariException("Invalid transition for"
              + " servicecomponenthost"
              + ", clusterName=" + cluster.getClusterName()
              + ", clusterId=" + cluster.getClusterId()
              + ", serviceName=" + sch.getServiceName()
              + ", componentName=" + sch.getServiceComponentName()
              + ", hostname=" + sch.getHostName()
              + ", currentState=" + oldSchState
              + ", newDesiredState=" + newState);
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
              + ", currentState=" + oldSchState
              + ", newDesiredState=" + newState);
        }
        changedScHosts.get(sc.getName()).get(newState).add(sch);
      }
    }

    if (seenNewStates.size() > 1) {
      // FIXME should we handle this scenario
      throw new IllegalArgumentException("Cannot handle different desired"
          + " state changes for a set of service components at the same time");
    }

    // TODO additional validation?

    // TODO if all components reach a common state, should service state be
    // modified?

    for (ServiceComponentRequest request : requests) {
      Cluster cluster = clusters.getCluster(request.getClusterName());
      Service s = cluster.getService(request.getServiceName());
      ServiceComponent sc = s.getServiceComponent(
          request.getComponentName());
      if (request.getConfigVersions() != null) {
        Map<String, Config> updated = new HashMap<String, Config>();

        for (Entry<String,String> entry :
          request.getConfigVersions().entrySet()) {
          Config config = cluster.getDesiredConfig(
              entry.getKey(), entry.getValue());
          updated.put(config.getType(), config);
        }

        if (!updated.isEmpty()) {
          sc.updateDesiredConfigs(updated);
          for (ServiceComponentHost sch :
              sc.getServiceComponentHosts().values()) {
            sch.deleteDesiredConfigs(updated.keySet());
            sch.persist();
          }
          sc.persist();
        }
      }
    }

    Cluster cluster = clusters.getCluster(clusterNames.iterator().next());

    return doStageCreation(cluster, null,
        changedComps, changedScHosts);
  }

  @Override
  public synchronized void updateHosts(Set<HostRequest> requests)
      throws AmbariException {

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
      return;
    }

    for (HostRequest request : requests) {
      if (request.getHostname() == null
          || request.getHostname().isEmpty()) {
        throw new IllegalArgumentException("Invalid arguments, hostname should"
            + " be provided");
      }

      if (LOG.isDebugEnabled()) {
        LOG.debug("Received a updateHost request"
            + ", hostname=" + request.getHostname()
            + ", request=" + request);
      }

      Host h = clusters.getHost(request.getHostname());

      try {
        //todo: the below method throws an exception when trying to create a duplicate mapping.
        //todo: this is done to detect duplicates during host create.  Unless it is allowable to
        //todo: add a host to a cluster by modifying the cluster_name prop, we should not do this mapping here.
        //todo: Determine if it is allowable to associate a host to a cluster via this mechanism.
        clusters.mapHostToCluster(request.getHostname(), request.getClusterName());
      } catch (DuplicateResourceException e) {
        // do nothing
      }

      if (null != request.getHostAttributes())
        h.setHostAttributes(request.getHostAttributes());

      if (null != request.getRackInfo()) {
        h.setRackInfo(request.getRackInfo());
      }
      
      if (null != request.getPublicHostName()) {
        h.setPublicHostName(request.getPublicHostName());
      }

      //todo: if attempt was made to update a property other than those
      //todo: that are allowed above, should throw exception
    }
  }

  @Override
  public synchronized RequestStatusResponse updateHostComponents(
      Set<ServiceComponentHostRequest> requests) throws AmbariException {

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
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
          || request.getComponentName() == null
          || request.getComponentName().isEmpty()
          || request.getHostname() == null
          || request.getHostname().isEmpty()) {
        throw new IllegalArgumentException("Invalid arguments"
            + ", cluster name, component name and host name should be"
            + " provided to update host components");
      }

      Cluster cluster = clusters.getCluster(request.getClusterName());

      if (request.getServiceName() == null
          || request.getServiceName().isEmpty()) {
        StackId stackId = cluster.getDesiredStackVersion();
        String serviceName =
            ambariMetaInfo.getComponentToService(stackId.getStackName(),
                stackId.getStackVersion(), request.getComponentName());
        if (LOG.isDebugEnabled()) {
          LOG.debug("Looking up service name for component"
              + ", componentName=" + request.getComponentName()
              + ", serviceName=" + serviceName);
        }

        if (serviceName == null
            || serviceName.isEmpty()) {
          throw new AmbariException("Could not find service for component"
              + ", componentName=" + request.getComponentName()
              + ", clusterName=" + cluster.getClusterName()
              + ", stackInfo=" + stackId.getStackId());
        }
        request.setServiceName(serviceName);
      }

      if (LOG.isDebugEnabled()) {
        LOG.debug("Received a createHostComponent request"
            + ", clusterName=" + request.getClusterName()
            + ", serviceName=" + request.getServiceName()
            + ", componentName=" + request.getComponentName()
            + ", hostname=" + request.getHostname()
            + ", request=" + request);
      }

      clusterNames.add(request.getClusterName());

      if (clusterNames.size() > 1) {
        throw new IllegalArgumentException("Updates to multiple clusters is not"
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
        throw new IllegalArgumentException("Invalid request contains duplicate"
            + " hostcomponents");
      }
      hostComponentNames.get(request.getClusterName())
          .get(request.getServiceName()).get(request.getComponentName())
          .add(request.getHostname());


      Service s = cluster.getService(request.getServiceName());
      ServiceComponent sc = s.getServiceComponent(
        request.getComponentName());
      ServiceComponentHost sch = sc.getServiceComponentHost(
        request.getHostname());
      State oldState = sch.getState();
      State newState = null;
      if (request.getDesiredState() != null) {
        newState = State.valueOf(request.getDesiredState());
        if (!newState.isValidDesiredState()) {
          throw new IllegalArgumentException("Invalid arguments, invalid"
              + " desired state, desiredState=" + newState.toString());
        }
      }

      if (request.getConfigVersions() != null) {
        safeToUpdateConfigsForServiceComponentHost(sch, oldState, newState);

        for (Entry<String,String> entry :
            request.getConfigVersions().entrySet()) {
          Config config = cluster.getDesiredConfig(
              entry.getKey(), entry.getValue());
          if (null == config) {
            throw new AmbariException("Trying to update servicecomponenthost"
                + " with invalid configs"
                + ", clusterName=" + cluster.getClusterName()
                + ", clusterId=" + cluster.getClusterId()
                + ", serviceName=" + s.getName()
                + ", componentName=" + sc.getName()
                + ", hostname=" + sch.getHostName()
                + ", invalidConfigType=" + entry.getKey()
                + ", invalidConfigTag=" + entry.getValue());
          }
        }
      }

      if (newState == null) {
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

      if (sc.isClientComponent() &&
          !newState.isValidClientComponentState()) {
        throw new IllegalArgumentException("Invalid desired state for a client"
            + " component");
      }

      seenNewStates.add(newState);

      State oldSchState = sch.getState();
      if (newState == oldSchState) {
        sch.setDesiredState(newState);
        if (LOG.isDebugEnabled()) {
          LOG.debug("Ignoring ServiceComponentHost"
              + ", clusterName=" + request.getClusterName()
              + ", serviceName=" + s.getName()
              + ", componentName=" + sc.getName()
              + ", hostname=" + sch.getHostName()
              + ", currentState=" + oldSchState
              + ", newDesiredState=" + newState);
        }
        continue;
      } 
      
      if (!isValidStateTransition(oldSchState, newState)) {
        throw new AmbariException("Invalid transition for"
            + " servicecomponenthost"
            + ", clusterName=" + cluster.getClusterName()
            + ", clusterId=" + cluster.getClusterId()
            + ", serviceName=" + sch.getServiceName()
            + ", componentName=" + sch.getServiceComponentName()
            + ", hostname=" + sch.getHostName()
            + ", currentState=" + oldSchState
            + ", newDesiredState=" + newState);
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
            + ", currentState=" + oldSchState
            + ", newDesiredState=" + newState);
      }
      changedScHosts.get(sc.getName()).get(newState).add(sch);
    }

    if (seenNewStates.size() > 1) {
      // FIXME should we handle this scenario
      throw new IllegalArgumentException("Cannot handle different desired"
          + " state changes for a set of service components at the same time");
    }


    // TODO additional validation?
    for (ServiceComponentHostRequest request : requests) {
      Cluster cluster = clusters.getCluster(request.getClusterName());
      Service s = cluster.getService(request.getServiceName());
      ServiceComponent sc = s.getServiceComponent(
          request.getComponentName());
      ServiceComponentHost sch = sc.getServiceComponentHost(
          request.getHostname());
      if (request.getConfigVersions() != null) {
        Map<String, Config> updated = new HashMap<String, Config>();

        for (Entry<String,String> entry : request.getConfigVersions().entrySet()) {
          Config config = cluster.getDesiredConfig(
              entry.getKey(), entry.getValue());
          updated.put(config.getType(), config);

          if (!updated.isEmpty()) {
            sch.updateDesiredConfigs(updated);
            sch.persist();
          }
        }
      }
    }

    Cluster cluster = clusters.getCluster(clusterNames.iterator().next());

    return doStageCreation(cluster, null,
        null, changedScHosts);
  }

  @Override
  public synchronized void updateUsers(Set<UserRequest> requests) throws AmbariException {
    for (UserRequest request : requests) {
      User u = users.getAnyUser(request.getUsername());
      if (null == u)
        continue;

      if (null != request.getOldPassword() && null != request.getPassword()) {
        users.modifyPassword(u.getUserName(), request.getOldPassword(),
            request.getPassword());
      }

      if (request.getRoles().size() > 0) {
        for (String role : u.getRoles()) {
          users.removeRoleFromUser(u, role);
        }

        for (String role : request.getRoles()) {
          users.addRoleToUser(u, role);
        }
      }

    }
  }

  @Override
  public synchronized void deleteCluster(ClusterRequest request)
      throws AmbariException {
    throw new AmbariException("Delete cluster not supported");

    /*
    if (request.getClusterName() == null
        || request.getClusterName().isEmpty()) {
      // FIXME throw correct error
      throw new AmbariException("Invalid arguments");
    }
    LOG.info("Received a delete cluster request"
        + ", clusterName=" + request.getClusterName());
    if (request.getHostNames() != null) {
      // FIXME treat this as removing a host from a cluster?
    } else {
      // deleting whole cluster
      clusters.deleteCluster(request.getClusterName());
    }
    */
  }

  @Override
  public RequestStatusResponse deleteServices(Set<ServiceRequest> request)
      throws AmbariException {
    throw new AmbariException("Delete services not supported");
  }

  @Override
  public RequestStatusResponse deleteComponents(
      Set<ServiceComponentRequest> request) throws AmbariException {
    throw new AmbariException("Delete components not supported");
  }

  @Override
  public void deleteHosts(Set<HostRequest> request)
      throws AmbariException {
    throw new AmbariException("Delete hosts not supported");
  }

  @Override
  public RequestStatusResponse deleteHostComponents(
      Set<ServiceComponentHostRequest> request) throws AmbariException {
    throw new AmbariException("Delete host components not supported");
  }

  @Override
  public void deleteUsers(Set<UserRequest> requests)
    throws AmbariException {

    for (UserRequest r : requests) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Received a delete user request"
            + ", username=" + r.getUsername());
      }
      User u = users.getAnyUser(r.getUsername());
      if (null != u)
        users.removeUser(u);
    }
  }

  @Override
  public Set<ActionResponse> getActions(Set<ActionRequest> request)
      throws AmbariException {
    Set<ActionResponse> responses = new HashSet<ActionResponse>();

    for (ActionRequest actionRequest : request) {
      if (actionRequest.getServiceName() == null) {
        LOG.warn("No service name specified - skipping request");
        //TODO throw error?
        continue;
      }
      ActionResponse actionResponse = new ActionResponse();
      actionResponse.setClusterName(actionRequest.getClusterName());
      actionResponse.setServiceName(actionRequest.getServiceName());
      if (actionMetadata.getActions(actionRequest.getServiceName()) != null
          && !actionMetadata.getActions(actionRequest.getServiceName())
              .isEmpty()) {
        actionResponse.setActionName(actionMetadata.getActions(
            actionRequest.getServiceName()).get(0));
      }
      responses.add(actionResponse);
    }

    return responses;
  }

  public Set<RequestStatusResponse> getRequestsByStatus(RequestsByStatusesRequest request) {

    //TODO implement.  Throw UnsupportedOperationException if it is not supported.
    return Collections.emptySet();
  }

  private RequestStatusResponse getRequestStatusResponse(long requestId) {
    RequestStatusResponse response = new RequestStatusResponse(requestId);
    List<HostRoleCommand> hostRoleCommands =
        actionManager.getRequestTasks(requestId);
    List<ShortTaskStatus> tasks = new ArrayList<ShortTaskStatus>();

    for (HostRoleCommand hostRoleCommand : hostRoleCommands) {
      tasks.add(new ShortTaskStatus(hostRoleCommand));
    }
    response.setTasks(tasks);

    return response;
  }

  @Override
  public Set<RequestStatusResponse> getRequestStatus(
      RequestStatusRequest request) throws AmbariException{
    Set<RequestStatusResponse> response = new HashSet<RequestStatusResponse>();
    if (request.getRequestId() == null) {
      RequestStatus requestStatus = RequestStatus.IN_PROGRESS;
      if (request.getRequestStatus() != null) {
        requestStatus = RequestStatus.valueOf(request.getRequestStatus());
      }
      if (LOG.isDebugEnabled()) {
        LOG.debug("Received a Get Request Status request"
            + ", requestId=null"
            + ", requestStatus=" + requestStatus);
      }
      List<Long> requestIds = actionManager.getRequestsByStatus(requestStatus);
      for (Long requestId : requestIds) {
        response.add(getRequestStatusResponse(requestId.longValue()));
      }
    } else {
      RequestStatusResponse requestStatusResponse = getRequestStatusResponse(
          request.getRequestId().longValue());

      //todo: correlate request with cluster
      if (requestStatusResponse.getTasks().size() == 0) {
        //todo: should be thrown lower in stack but we only want to throw if id was specified
        //todo: and we currently iterate over all id's and invoke for each if id is not specified
        throw new ObjectNotFoundException("Request resource doesn't exist.");
      } else {
        response.add(requestStatusResponse);
      }
    }
    return response;
  }

  @Override
  public Set<TaskStatusResponse> getTaskStatus(Set<TaskStatusRequest> requests)
      throws AmbariException {

    Collection<Long> requestIds = new ArrayList<Long>();
    Collection<Long> taskIds = new ArrayList<Long>();

    for (TaskStatusRequest request : requests) {
      if (request.getTaskId() != null) {
        taskIds.add(request.getTaskId());
      } else {
        requestIds.add(request.getRequestId());
      }
    }

    Set<TaskStatusResponse> responses = new HashSet<TaskStatusResponse>();
    for (HostRoleCommand command : actionManager.getTasksByRequestAndTaskIds(requestIds, taskIds)) {
      responses.add(new TaskStatusResponse(command));
    }

    return responses;
  }

  @Override
  public Set<ClusterResponse> getClusters(Set<ClusterRequest> requests) throws AmbariException {
    Set<ClusterResponse> response = new HashSet<ClusterResponse>();
    for (ClusterRequest request : requests) {
      try {
        response.addAll(getClusters(request));
      } catch (ClusterNotFoundException e) {
        if (requests.size() == 1) {
          // only throw exception if 1 request.
          // there will be > 1 request in case of OR predicate
          throw e;
        }
      }
    }
    return response;
  }

  @Override
  public Set<ServiceResponse> getServices(Set<ServiceRequest> requests)
      throws AmbariException {
    Set<ServiceResponse> response = new HashSet<ServiceResponse>();
    for (ServiceRequest request : requests) {
      try {
        response.addAll(getServices(request));
      } catch (ServiceNotFoundException e) {
        if (requests.size() == 1) {
          // only throw exception if 1 request.
          // there will be > 1 request in case of OR predicate
          throw e;
        }
      }
    }
    return response;
  }

  @Override
  public Set<ServiceComponentResponse> getComponents(
      Set<ServiceComponentRequest> requests) throws AmbariException {
    Set<ServiceComponentResponse> response =
        new HashSet<ServiceComponentResponse>();
    for (ServiceComponentRequest request : requests) {
      try {
        response.addAll(getComponents(request));
      } catch (ServiceComponentNotFoundException e) {
        if (requests.size() == 1) {
          // only throw exception if 1 request.
          // there will be > 1 request in case of OR predicate
          throw e;
        }
      }
    }
    return response;
  }

  @Override
  public Set<HostResponse> getHosts(Set<HostRequest> requests)
      throws AmbariException {
    Set<HostResponse> response = new HashSet<HostResponse>();
    for (HostRequest request : requests) {
      try {
        response.addAll(getHosts(request));
      } catch (HostNotFoundException e) {
        if (requests.size() == 1) {
          // only throw exception if 1 request.
          // there will be > 1 request in case of OR predicate
          throw e;
        }
      }
    }
    return response;
  }

  @Override
  public Set<ServiceComponentHostResponse> getHostComponents(
      Set<ServiceComponentHostRequest> requests) throws AmbariException {
    Set<ServiceComponentHostResponse> response =
        new HashSet<ServiceComponentHostResponse>();
    for (ServiceComponentHostRequest request : requests) {
      try {
        response.addAll(getHostComponents(request));
      } catch (ServiceComponentHostNotFoundException e) {
        if (requests.size() == 1) {
          // only throw exception if 1 request.
          // there will be > 1 request in case of OR predicate
          throw e;
        }
      } catch (ServiceNotFoundException e) {
        if (requests.size() == 1) {
          // only throw exception if 1 request.
          // there will be > 1 request in case of OR predicate
          // In 'OR' case, a host_component may be included in predicate
          // that has no corresponding service
          throw e;
        }
      } catch (ServiceComponentNotFoundException e) {
        if (requests.size() == 1) {
          // only throw exception if 1 request.
          // there will be > 1 request in case of OR predicate
          // In 'OR' case, a host_component may be included in predicate
          // that has no corresponding component
          throw e;
        }
      } catch (ParentObjectNotFoundException e) {
        // If there is only one request, always throw exception.
        // There will be > 1 request in case of OR predicate.

        // For HostNotFoundException, only throw exception if host_name is
        // provided in URL.  If host_name is part of query, don't throw exception.
        boolean throwException = true;
        if (requests.size() > 1 && HostNotFoundException.class.isInstance(e.getCause())) {
          for (ServiceComponentHostRequest r : requests) {
            if (r.getHostname() == null) {
              // host_name provided in query since all requests don't have host_name set
              throwException = false;
              break;
            }
          }
        }
        if (throwException) throw e;
      }
    }
    return response;
  }

  @Override
  public Set<ConfigurationResponse> getConfigurations(
      Set<ConfigurationRequest> requests) throws AmbariException {
    Set<ConfigurationResponse> response =
        new HashSet<ConfigurationResponse>();
    for (ConfigurationRequest request : requests) {
      response.addAll(getConfigurations(request));
    }
    return response;
  }

  @Override
  public Set<UserResponse> getUsers(Set<UserRequest> requests)
      throws AmbariException {

    Set<UserResponse> responses = new HashSet<UserResponse>();

    for (UserRequest r : requests) {

      if (LOG.isDebugEnabled()) {
        LOG.debug("Received a getUsers request"
            + ", userRequest=" + r.toString());
      }
      // get them all
      if (null == r.getUsername()) {
        for (User u : users.getAllUsers()) {
          UserResponse resp = new UserResponse(u.getUserName(), u.isLdapUser());
          resp.setRoles(new HashSet<String>(u.getRoles()));
          responses.add(resp);
        }
      } else {

        User u = users.getAnyUser(r.getUsername());
        if (null == u) {
          if (requests.size() == 1) {
            // only throw exceptin if there is a single request
            // if there are multiple requests, this indicates an OR predicate
            throw new ObjectNotFoundException("Cannot find user '"
                + r.getUsername() + "'");
          }
        } else {
          UserResponse resp = new UserResponse(u.getUserName(), u.isLdapUser());
          resp.setRoles(new HashSet<String>(u.getRoles()));
          responses.add(resp);
        }
      }
    }

    return responses;
  }

  @Override
  public Map<String, String> getHostComponentDesiredConfigMapping(ServiceComponentHostRequest request)
    throws AmbariException {

    Map<String, String> map = new HashMap<String, String>();

    for (ServiceComponentHostResponse r : getHostComponents(request)) {
      map.putAll(r.getDesiredConfigs());
    }

    return map;
  }

  private String getClientHostForRunningAction(Cluster cluster,
      Service service) throws AmbariException {
    StackId stackId = service.getDesiredStackVersion();
    ComponentInfo compInfo =
        ambariMetaInfo.getServiceInfo(stackId.getStackName(),
            stackId.getStackVersion(), service.getName()).getClientComponent();
    if (compInfo != null) {
      try {
        ServiceComponent serviceComponent =
            service.getServiceComponent(compInfo.getName());
        if (!serviceComponent.getServiceComponentHosts().isEmpty()) {
          return serviceComponent.getServiceComponentHosts()
              .keySet().iterator().next();
        }
      } catch (ServiceComponentNotFoundException e) {
        LOG.warn("Could not find required component to run action"
            + ", clusterName=" + cluster.getClusterName()
            + ", serviceName=" + service.getName()
            + ", componentName=" + compInfo.getName());


      }
    }

    // any component will do
    Map<String, ServiceComponent> components = service.getServiceComponents();
    if (components.isEmpty()) {
      return null;
    }

    for (ServiceComponent serviceComponent : components.values()) {
      if (serviceComponent.getServiceComponentHosts().isEmpty()) {
        continue;
      }
      return serviceComponent.getServiceComponentHosts()
          .keySet().iterator().next();
    }
    return null;
  }

  private void addServiceCheckAction(ActionRequest actionRequest, Stage stage)
      throws AmbariException {
    String clusterName = actionRequest.getClusterName();
    String componentName = actionMetadata.getClient(actionRequest
        .getServiceName());

    String hostName;
    if (componentName != null) {
      Map<String, ServiceComponentHost> components = clusters
          .getCluster(clusterName).getService(actionRequest.getServiceName())
          .getServiceComponent(componentName).getServiceComponentHosts();

      if (components.isEmpty()) {
        throw new AmbariException("Hosts not found, component="
            + componentName + ", service=" + actionRequest.getServiceName()
            + ", cluster=" + clusterName);
      }

      hostName = components.keySet().iterator().next();
    } else {
      Map<String, ServiceComponent> components = clusters
          .getCluster(clusterName).getService(actionRequest.getServiceName())
          .getServiceComponents();

      if (components.isEmpty()) {
        throw new AmbariException("Components not found, service="
            + actionRequest.getServiceName() + ", cluster=" + clusterName);
      }

      ServiceComponent serviceComponent = components.values().iterator()
          .next();

      if (serviceComponent.getServiceComponentHosts().isEmpty()) {
        throw new AmbariException("Hosts not found, component="
            + serviceComponent.getName() + ", service="
            + actionRequest.getServiceName() + ", cluster=" + clusterName);
      }

      hostName = serviceComponent.getServiceComponentHosts().keySet()
          .iterator().next();
    }

    stage.addHostRoleExecutionCommand(hostName, Role.valueOf(actionRequest
        .getActionName()), RoleCommand.EXECUTE,
        new ServiceComponentHostOpInProgressEvent(componentName, hostName,
            System.currentTimeMillis()), clusterName, actionRequest
            .getServiceName());

    stage.getExecutionCommandWrapper(hostName, actionRequest.getActionName()).getExecutionCommand()
        .setRoleParams(actionRequest.getParameters());

    Map<String, Map<String, String>> configurations = new TreeMap<String, Map<String, String>>();
    Map<String, Config> allConfigs = clusters.getCluster(clusterName)
        .getService(actionRequest.getServiceName()).getDesiredConfigs();
    if (allConfigs != null) {
      for (Map.Entry<String, Config> entry: allConfigs.entrySet()) {
        configurations.put(entry.getValue().getType(), entry.getValue().getProperties());
      }
    }
    
    stage.getExecutionCommandWrapper(hostName,
        actionRequest.getActionName()).getExecutionCommand()
        .setConfigurations(configurations); 
    
    // Generate cluster host info
    stage
        .getExecutionCommandWrapper(hostName, actionRequest.getActionName())
        .getExecutionCommand()
        .setClusterHostInfo(
            StageUtils.getClusterHostInfo(clusters.getCluster(clusterName)));
  }

  private void addDecommissionDatanodeAction(
      ActionRequest decommissionRequest, Stage stage)
      throws AmbariException {
    // Find hdfs admin host, just decommission from namenode.
    String clusterName = decommissionRequest.getClusterName();
    String serviceName = decommissionRequest.getServiceName();
    String namenodeHost = clusters.getCluster(clusterName)
        .getService(serviceName).getServiceComponent(Role.NAMENODE.toString())
        .getServiceComponentHosts().keySet().iterator().next();

    String excludeFileTag = null;
    if (decommissionRequest.getParameters() != null
        && (decommissionRequest.getParameters().get("excludeFileTag") != null)) {
      excludeFileTag = decommissionRequest.getParameters()
          .get("excludeFileTag");
    }

    if (excludeFileTag == null) {
      throw new IllegalArgumentException("No exclude file specified"
          + " when decommissioning datanodes");
    }

    Config config = clusters.getCluster(clusterName).getDesiredConfig(
        "hdfs-exclude-file", excludeFileTag);

    Map<String, Map<String, String>> configurations =
        new TreeMap<String, Map<String, String>>();
    configurations.put(config.getType(), config.getProperties());

    Map<String, Config> hdfsSiteConfig = clusters.getCluster(clusterName).getService("HDFS")
        .getDesiredConfigs();
    if (hdfsSiteConfig != null) {
      for (Map.Entry<String, Config> entry: hdfsSiteConfig.entrySet()) {
        configurations
          .put(entry.getValue().getType(), entry.getValue().getProperties());
      }
    }
    
    stage.addHostRoleExecutionCommand(
        namenodeHost,
        Role.DECOMMISSION_DATANODE,
        RoleCommand.EXECUTE,
        new ServiceComponentHostOpInProgressEvent(Role.DECOMMISSION_DATANODE
            .toString(), namenodeHost, System.currentTimeMillis()),
        clusterName, serviceName);
    stage.getExecutionCommandWrapper(namenodeHost,
        Role.DECOMMISSION_DATANODE.toString()).getExecutionCommand()
        .setConfigurations(configurations);
    
  }

  @Override
  public RequestStatusResponse createActions(Set<ActionRequest> request)
      throws AmbariException {
    String clusterName = null;

    String logDir = ""; //TODO empty for now

    for (ActionRequest actionRequest : request) {
      if (actionRequest.getClusterName() == null
          || actionRequest.getClusterName().isEmpty()
          || actionRequest.getServiceName() == null
          || actionRequest.getServiceName().isEmpty()
          || actionRequest.getActionName() == null
          || actionRequest.getActionName().isEmpty()) {
        throw new AmbariException("Invalid action request : " + "cluster="
            + actionRequest.getClusterName() + ", service="
            + actionRequest.getServiceName() + ", action="
            + actionRequest.getActionName());
      } else if (clusterName == null) {
        clusterName = actionRequest.getClusterName();
      } else if (!clusterName.equals(actionRequest.getClusterName())) {
        throw new AmbariException("Requests for different clusters found");
      }
    }

    Stage stage = stageFactory.createNew(actionManager.getNextRequestId(),
        logDir, clusterName);
    stage.setStageId(0);
    for (ActionRequest actionRequest : request) {
      if (actionRequest.getActionName().contains("SERVICE_CHECK")) {
        addServiceCheckAction(actionRequest, stage);
      } else if (actionRequest.getActionName().equals("DECOMMISSION_DATANODE")) {
        addDecommissionDatanodeAction(actionRequest, stage);
      } else {
        throw new AmbariException("Unsupported action");
      }
    }
    RoleGraph rg = new RoleGraph(rco);
    rg.build(stage);
    List<Stage> stages = rg.getStages();
    if (stages != null && !stages.isEmpty()) {
      actionManager.sendActions(stages);
      return getRequestStatusResponse(stage.getRequestId());
    } else {
      throw new AmbariException("Stage was not created");
    }
  }
}
