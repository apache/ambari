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

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import com.google.inject.persist.Transactional;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.DuplicateResourceException;
import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.ParentObjectNotFoundException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.ServiceComponentHostNotFoundException;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.StackAccessException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.RequestStatus;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.metadata.RoleCommandOrder;
import org.apache.ambari.server.security.authorization.User;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.serveraction.ServerAction;
import org.apache.ambari.server.stageplanner.RoleGraph;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.OperatingSystemInfo;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostEvent;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostInstallEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostMaintenanceEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpInProgressEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostRestoreEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStartEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStopEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostUpgradeEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class AmbariManagementControllerImpl implements
    AmbariManagementController {

  private final static Logger LOG =
      LoggerFactory.getLogger(AmbariManagementControllerImpl.class);

  /**
   * Property name of request context.
   */
  private static final String REQUEST_CONTEXT_PROPERTY = "context";

  private final Clusters clusters;

  private String baseLogDir = "/tmp/ambari";

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
  @Inject
  private HostsMap hostsMap;
  @Inject
  private Configuration configs;

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

    if (configs != null) {
      this.jdkResourceUrl = "http://" + masterHostname + ":"
          + configs.getClientApiPort()
          + JDK_RESOURCE_LOCATION;
    } else {
    		this.jdkResourceUrl = null;
    }
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
      throw new StackAccessException("stackName=" + stackId.getStackName() + ", stackVersion=" + stackId.getStackVersion());
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
      StackId newStackId = new StackId(request.getStackVersion());
      c.setDesiredStackVersion(newStackId);
      clusters.setCurrentStackVersion(request.getClusterName(), newStackId);
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

    Map<String, Set<String>> hostClustersMap = new HashMap<String, Set<String>>();
    Map<String, Map<String, String>> hostAttributes = new HashMap<String, Map<String, String>>();
    for (HostRequest request : requests) {
      if (request.getHostname() != null) {
        Set<String> clusters = new HashSet<String>();
        clusters.add(request.getClusterName());
        hostClustersMap.put(request.getHostname(), clusters);
        if (request.getHostAttributes() != null) {
          hostAttributes.put(request.getHostname(), request.getHostAttributes());
        }
      }
    }
    clusters.updateHostWithClusterAndAttributes(hostClustersMap, hostAttributes);
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
      validateServiceComponentHostRequest(request);

      Cluster cluster;
      try {
        cluster = clusters.getCluster(request.getClusterName());
      } catch (ClusterNotFoundException e) {
        throw new ParentObjectNotFoundException(
            "Attempted to add a host_component to a cluster which doesn't exist: ", e);
      }

      if (StringUtils.isEmpty(request.getServiceName())) {
        request.setServiceName(findServiceName(cluster, request.getComponentName()));
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

  @Override
  public synchronized void createConfiguration(
      ConfigurationRequest request) throws AmbariException {
    if (null == request.getClusterName() || request.getClusterName().isEmpty()
        || null == request.getType() || request.getType().isEmpty()
        || null == request.getVersionTag() || request.getVersionTag().isEmpty()
        || null == request.getProperties() || request.getProperties().isEmpty()) {
      throw new IllegalArgumentException("Invalid Arguments,"
          + " clustername, config type, config version and configs should not"
          + " be null or empty");
    }

    Cluster cluster = clusters.getCluster(request.getClusterName());

    Map<String, Config> configs = cluster.getConfigsByType(
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
        request.getProperties());
    config.setVersionTag(request.getVersionTag());

    config.persist();

    cluster.addConfig(config);
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

  private Stage createNewStage(Cluster cluster, long requestId, String requestContext) {
    String logDir = baseLogDir + File.pathSeparator + requestId;
    Stage stage = new Stage(requestId, logDir, cluster.getClusterName(), requestContext);
    return stage;
  }

  private void createHostAction(Cluster cluster,
      Stage stage, ServiceComponentHost scHost,
      Map<String, Map<String, String>> configurations,
      Map<String, Map<String, String>> configTags,
      RoleCommand command,
      Map<String, String> commandParams,
      ServiceComponentHostEvent event) throws AmbariException {

    stage.addHostRoleExecutionCommand(scHost.getHostName(), Role.valueOf(scHost
        .getServiceComponentName()), command,
        event, scHost.getClusterName(),
        scHost.getServiceName());
    ExecutionCommand execCmd = stage.getExecutionCommandWrapper(scHost.getHostName(),
        scHost.getServiceComponentName()).getExecutionCommand();

    // Generate cluster host info
    execCmd.setClusterHostInfo(
        StageUtils.getClusterHostInfo(cluster, hostsMap, injector));

    Host host = clusters.getHost(scHost.getHostName());

    execCmd.setConfigurations(configurations);
    execCmd.setConfigurationTags(configTags);
    execCmd.setCommandParams(commandParams);

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
    params.put("stack_version", stackId.getStackVersion());
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
      ClusterResponse cr = c.convertToResponse();
      cr.setDesiredConfigs(c.getDesiredConfigs());
      cr.setActualConfigs(c.getActualConfigs());
      response.add(cr);
      return response;
    } else if (request.getClusterId() != null) {
      Cluster c = clusters.getClusterById(request.getClusterId());
      ClusterResponse cr = c.convertToResponse();
      cr.setDesiredConfigs(c.getDesiredConfigs());
      cr.setActualConfigs(c.getActualConfigs());
      response.add(cr);
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
      try {
        hosts.add(clusters.getHost(request.getHostname()));
      } catch (HostNotFoundException e) {
        // add cluster name
        throw new HostNotFoundException(clusterName, hostName);
      }
    }

    for (Host h : hosts) {
      if (clusterName != null) {
        if (clusters.getClustersForHost(h.getHostName()).contains(cluster)) {
          HostResponse r = h.convertToResponse();
          r.setClusterName(clusterName);
          r.setDesiredConfigs(h.getDesiredConfigs(cluster.getClusterId()));

          response.add(r);
        } else if (hostName != null) {
          throw new HostNotFoundException(clusterName, hostName);
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
      Config config = cluster.getConfig(request.getType(),
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
        Map<String, Config> configs = cluster.getConfigsByType(
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
  public synchronized RequestStatusResponse updateCluster(ClusterRequest request,
                                                          Map<String, String> requestProperties)
      throws AmbariException {
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

    final Cluster cluster = clusters.getCluster(request.getClusterName());

    // set or create configuration mapping (and optionally create the map of properties)
    if (null != request.getDesiredConfig()) {
      ConfigurationRequest cr = request.getDesiredConfig();

      if (null != cr.getProperties() && cr.getProperties().size() > 0) {
        cr.setClusterName(cluster.getClusterName());
        createConfiguration(cr);
      }

      Config baseConfig = cluster.getConfig(cr.getType(), cr.getVersionTag());
      if (null != baseConfig) {
        cluster.addDesiredConfig(baseConfig);
      }
    }

    StackId currentVersion = cluster.getCurrentStackVersion();
    StackId desiredVersion = cluster.getDesiredStackVersion();
    String requestedVersionString = request.getStackVersion();
    StackId requestedVersion = null;

    // Set the current version value if its not already set
    if (currentVersion == null) {
      cluster.setCurrentStackVersion(desiredVersion);
      currentVersion = cluster.getCurrentStackVersion();
    }

    boolean requiresHostListUpdate =
        request.getHostNames() != null && !request.getHostNames().isEmpty();
    // TODO Should upgrade be allowed to upgrade all un-upgraded hosts
    // even if the cluster says its upgraded
    boolean requiresVersionUpdate = requestedVersionString != null
        && !requestedVersionString.isEmpty();
    if (requiresVersionUpdate) {
      LOG.info("Received a cluster update request"
          + ", clusterName=" + request.getClusterName()
          + ", request=" + request);
      requestedVersion = new StackId(requestedVersionString);
      if (!requestedVersion.getStackName().equals(currentVersion.getStackName())) {
        throw new AmbariException("Upgrade not possible between different stacks.");
      }
      requiresVersionUpdate = !currentVersion.equals(requestedVersion);
      if(!requiresVersionUpdate) {
        LOG.info("The cluster is already at " + currentVersion);
      }
    }

    if (requiresVersionUpdate && requiresHostListUpdate) {
      throw new IllegalArgumentException("Invalid arguments, "
          + "cluster version cannot be upgraded"
          + " along with host list modifications");
    }

    if (requiresHostListUpdate) {
      clusters.mapHostsToCluster(
          request.getHostNames(), request.getClusterName());
    }

    if (requiresVersionUpdate) {
      LOG.info("Upgrade cluster request received for stack " + requestedVersion);
      boolean retry = false;
      if (0 == currentVersion.compareTo(desiredVersion)) {
        if (1 != requestedVersion.compareTo(currentVersion)) {
          throw new AmbariException("Target version : " + requestedVersion
              + " must be greater than current version : " + currentVersion);
        } else {
          StackInfo stackInfo =
              ambariMetaInfo.getStackInfo(requestedVersion.getStackName(), requestedVersion.getStackVersion());
          if (stackInfo == null) {
            throw new AmbariException("Target version : " + requestedVersion
                + " is not a recognized version");
          }
          if(!isUpgradeAllowed(stackInfo, currentVersion))
          {
            throw new AmbariException("Upgrade is not allowed from " + currentVersion
                + " to the target version " + requestedVersion);
          }
        }
      } else {
        retry = true;
        LOG.info("Received upgrade request is a retry.");
        if (0 != requestedVersion.compareTo(desiredVersion)) {
          throw new AmbariException("Upgrade in progress to target version : "
              + desiredVersion
              + ". Illegal request to upgrade to : " + requestedVersion);
        }
      }

      checkIfActiveComponentsExist(cluster, currentVersion);

      checkIfAnotherUpgradeCommandIsActive();

      // TODO Ensure no other upgrade is active
      /**
       * There exists no active upgrade. Perform a final check of current stack version
       * and proceed if upgrade is still required. Upgrade is idempotent so this check
       * is only to avoid potentially expensive stage creation.
       */
      cluster.refresh();
      if (requestedVersion.equals(cluster.getCurrentStackVersion())) {
        LOG.info("Update cluster request version matches the current"
                  + ", version=" + request);
        return null;
      }

      if (!retry) {
        cluster.setDesiredStackVersion(requestedVersion);
        for (Service service : cluster.getServices().values()) {
          service.setDesiredStackVersion(requestedVersion);
          for (ServiceComponent component : service.getServiceComponents().values()) {
            component.setDesiredStackVersion(requestedVersion);
            for (ServiceComponentHost componentHost : component.getServiceComponentHosts().values()) {
              componentHost.setDesiredStackVersion(requestedVersion);
            }
          }
        }
      }

      Map<State, List<Service>> changedServices
          = new HashMap<State, List<Service>>();
      Map<State, List<ServiceComponent>> changedComps =
          new HashMap<State, List<ServiceComponent>>();
      Map<String, Map<State, List<ServiceComponentHost>>> changedScHosts =
          new HashMap<String, Map<State, List<ServiceComponentHost>>>();

      LOG.info("Identifying components to upgrade.");
      fillComponentsToUpgrade(request, cluster, changedServices, changedComps, changedScHosts);
      Map<String, String> requestParameters = new HashMap<String, String>();
      requestParameters.put(Configuration.UPGRADE_TO_STACK, gson.toJson(requestedVersion));
      requestParameters.put(Configuration.UPGRADE_FROM_STACK, gson.toJson(currentVersion));

      LOG.info("Creating stages for upgrade.");
      List<Stage> stages = doStageCreation(cluster, changedServices, changedComps, changedScHosts,
          requestParameters, requestProperties.get(REQUEST_CONTEXT_PROPERTY), false);

      if (stages == null || stages.isEmpty()) {
        return null;
      }

      addFinalizeUpgradeAction(cluster, stages);
      persistStages(stages);
      updateServiceStates(changedServices, changedComps, changedScHosts);
      long requestId = stages.get(0).getRequestId();
      LOG.info(stages.size() + " stages created for upgrade and the request id is " + requestId);
      return getRequestStatusResponse(requestId);
    }

    return null;
  }

  private void checkIfAnotherUpgradeCommandIsActive() throws AmbariException {
    List<Long> requestIds = actionManager.getRequestsByStatus(RequestStatus.IN_PROGRESS);
    if (requestIds != null) {
      for (Long requestId : requestIds) {
        List<HostRoleCommand> commands = actionManager.getRequestTasks(requestId);
        if (commands != null) {
          for (HostRoleCommand command : commands) {
            if (command.getRoleCommand() == RoleCommand.UPGRADE
                && (command.getStatus() == HostRoleStatus.QUEUED
                || command.getStatus() == HostRoleStatus.PENDING
                || command.getStatus() == HostRoleStatus.IN_PROGRESS)) {
              throw new AmbariException("A prior upgrade request with id " + requestId
                  + " is in progress. Upgrade can "
                  + "only be retried after the prior command has completed.");
            }
          }
        }
      }
    }
  }

  private void addFinalizeUpgradeAction(Cluster cluster, List<Stage> stages) throws AmbariException {
    // Add server side action as the last Stage
    Stage lastStage = stages.get(stages.size() - 1);
    Stage newStage = createNewStage(cluster, lastStage.getRequestId(), "finalize upgrade");
    newStage.setStageId(lastStage.getStageId() + 1);

    // Add an arbitrary host name as server actions are executed on the server
    String hostName = lastStage.getOrderedHostRoleCommands().get(0).getHostName();

    Map<String, String> payload = new HashMap<String, String>();
    payload.put(ServerAction.PayloadName.CLUSTER_NAME, cluster.getClusterName());
    payload.put(ServerAction.PayloadName.CURRENT_STACK_VERSION, cluster.getDesiredStackVersion().getStackId());

    ServiceComponentHostUpgradeEvent event = new ServiceComponentHostUpgradeEvent(
        Role.AMBARI_SERVER_ACTION.toString(), hostName,
        System.currentTimeMillis(), cluster.getDesiredStackVersion().getStackId());
    newStage.addServerActionCommand(ServerAction.Command.FINALIZE_UPGRADE, Role.AMBARI_SERVER_ACTION,
        RoleCommand.EXECUTE, cluster.getClusterName(), event, hostName);
    ExecutionCommand execCmd = newStage.getExecutionCommandWrapper(hostName,
        Role.AMBARI_SERVER_ACTION.toString()).getExecutionCommand();

    execCmd.setCommandParams(payload);
    stages.add(newStage);
  }

  private boolean isUpgradeAllowed(StackInfo requestedStackInfo, StackId currentStackId) {
    String minUpgradeVersion = requestedStackInfo.getMinUpgradeVersion();
    if (minUpgradeVersion != null && !minUpgradeVersion.isEmpty()) {
      StackId minUpgradeStackId =
          new StackId(currentStackId.getStackName(), minUpgradeVersion);
      if (currentStackId.compareTo(minUpgradeStackId) >= 0) {
        return true;
      }
    }

    return false;
  }

  private void fillComponentsToUpgrade(ClusterRequest request, Cluster cluster,
           Map<State, List<Service>> changedServices, Map<State, List<ServiceComponent>> changedComps,
           Map<String, Map<State, List<ServiceComponentHost>>> changedScHosts) throws AmbariException {
    for (Service service : cluster.getServices().values()) {
      State oldState = service.getDesiredState();
      State newState = State.INSTALLED;

      if (!isValidDesiredStateTransition(oldState, newState)) {
        throw new AmbariException("Invalid transition for"
            + " service"
            + ", clusterName=" + cluster.getClusterName()
            + ", clusterId=" + cluster.getClusterId()
            + ", serviceName=" + service.getName()
            + ", currentDesiredState=" + oldState
            + ", newDesiredState=" + newState);
      }
      changedServices.put(newState, new ArrayList<Service>());
      changedServices.get(newState).add(service);

      for (ServiceComponent sc : service.getServiceComponents().values()) {
        State oldScState = sc.getDesiredState();
        if (newState != oldScState) {
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
          changedComps.put(newState, new ArrayList<ServiceComponent>());
          changedComps.get(newState).add(sc);
        }
        LOG.info("Handling upgrade to ServiceComponent"
            + ", clusterName=" + request.getClusterName()
            + ", serviceName=" + service.getName()
            + ", componentName=" + sc.getName()
            + ", currentDesiredState=" + oldScState
            + ", newDesiredState=" + newState);

        for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
          State currSchState = sch.getState();
          if (sch.getStackVersion().equals(sch.getDesiredStackVersion())
              && newState == currSchState) {
            LOG.info("Requesting upgrade for already upgraded ServiceComponentHost"
                + ", clusterName=" + request.getClusterName()
                + ", serviceName=" + service.getName()
                + ", componentName=" + sc.getName()
                + ", hostname=" + sch.getHostName()
                + ", currentState=" + currSchState
                + ", newDesiredState=" + newState
                + ", currentDesiredState=" + sch.getStackVersion()
                + ", newDesiredVersion=" + sch.getDesiredStackVersion());
          }

          sch.setState(State.UPGRADING);
          sch.setDesiredState(newState);
          if (!changedScHosts.containsKey(sc.getName())) {
            changedScHosts.put(sc.getName(),
                new HashMap<State, List<ServiceComponentHost>>());
          }
          changedScHosts.get(sc.getName()).put(newState,
              new ArrayList<ServiceComponentHost>());
          changedScHosts.get(sc.getName()).get(newState).add(sch);

          LOG.info("Handling update to ServiceComponentHost"
              + ", clusterName=" + request.getClusterName()
              + ", serviceName=" + service.getName()
              + ", componentName=" + sc.getName()
              + ", hostname=" + sch.getHostName()
              + ", currentState=" + currSchState
              + ", newDesiredState=" + newState);
        }
      }
    }
  }

  private void checkIfActiveComponentsExist(Cluster c, StackId currentStackId)
      throws AmbariException {
    String stackName = currentStackId.getStackName();
    String stackVersion = currentStackId.getStackVersion();
    StringBuilder sb = new StringBuilder("Upgrade needs all services to be stopped. ");
    for (Service service : c.getServices().values()) {
      if (service.getDesiredState() != State.INSTALLED) {
        sb.append("Service " + service.getName() + " is not stopped.");
        throw new AmbariException(sb.toString());
      }
      for (ServiceComponent component : service.getServiceComponents().values()) {
        if (component.getDesiredState() != State.INSTALLED) {
          sb.append("Component " + component.getName() + " of service "
              + service.getName() + " is not stopped.");
          throw new AmbariException(sb.toString());
        }
        for (ServiceComponentHost componentHost : component.getServiceComponentHosts().values()) {
          if (componentHost.getDesiredState() != State.INSTALLED) {
            sb.append("Component " + component.getName() + " of service "
                + service.getName() +  " on host "
                + componentHost.getHostName() + " is not stopped.");
            throw new AmbariException(sb.toString());
          }
          if(componentHost.getState() == State.STARTED) {
            ComponentInfo compInfo = ambariMetaInfo.getComponent(stackName, stackVersion,
                componentHost.getServiceName(), componentHost.getServiceComponentName());
            if(compInfo.isMaster()) {
              sb.append("Component " + component.getName() + " of service "
                  + service.getName() +  " on host "
                  + componentHost.getHostName() + " is not yet stopped.");
              throw new AmbariException(sb.toString());
            }
          }
        }
      }
    }
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

  private Set<String> getServicesForSmokeTests(Cluster cluster,
             Map<State, List<Service>> changedServices,
             Map<String, Map<State, List<ServiceComponentHost>>> changedScHosts,
             boolean runSmokeTest) throws AmbariException {

    Set<String> smokeTestServices = new HashSet<String>();

    if (changedServices != null) {
      for (Entry<State, List<Service>> entry : changedServices.entrySet()) {
        if (State.STARTED != entry.getKey()) {
          continue;
        }
        for (Service s : entry.getValue()) {
          if (runSmokeTest && (State.INSTALLED == s.getDesiredState())) {
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
          if (State.INSTALLED != sch.getState()) {
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
      if (runSmokeTest && (entry.getValue().size() > 1)) {
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
        if (runSmokeTest && compInfo.isMaster()) {
          smokeTestServices.add(serviceName);
        }

        // FIXME if master check if we need to run a smoke test for the master
      }
    }
    return smokeTestServices;
  }

  private void addClientSchForReinstall(Cluster cluster,
            Map<State, List<Service>> changedServices,
            Map<String, Map<State, List<ServiceComponentHost>>> changedScHosts)
            throws AmbariException {

    Set<String> services = new HashSet<String>();

    if (changedServices != null) {
      for (Entry<State, List<Service>> entry : changedServices.entrySet()) {
        if (State.STARTED != entry.getKey()) {
          continue;
        }
        for (Service s : entry.getValue()) {
          if (State.INSTALLED == s.getDesiredState()) {
            services.add(s.getName());
          }
        }
      }
    }

    if (services == null || services.isEmpty())
      return;

    // Flatten changed Schs that are going to be Started
    List<ServiceComponentHost> existingSchs = new
      ArrayList<ServiceComponentHost>();
    if (changedScHosts != null && !changedScHosts.isEmpty()) {
      for (String sc : changedScHosts.keySet()) {
        for (State s : changedScHosts.get(sc).keySet())
          if (s == State.STARTED)
            existingSchs.addAll(changedScHosts.get(sc).get(s));
      }
    }

    Map<String, List<ServiceComponentHost>> clientSchs = new
      HashMap<String, List<ServiceComponentHost>>();

    for (String serviceName : services) {
      Service s = cluster.getService(serviceName);
      for (String component : s.getServiceComponents().keySet()) {
        List<ServiceComponentHost> potentialHosts = null;
        ServiceComponent sc = s.getServiceComponents().get(component);
        if (sc.isClientComponent()) {
          potentialHosts = new ArrayList<ServiceComponentHost>();
          // Check if the Client components are in the list of changed hosts
          if (existingSchs != null && !existingSchs.isEmpty()) {
            for (ServiceComponentHost potentialSch : sc
              .getServiceComponentHosts().values()) {
              boolean addSch = true;
              // Ignore the Sch if same service has changed on the same host
              for (ServiceComponentHost existingSch : existingSchs) {
                if (potentialSch.getHostName().equals(existingSch
                  .getHostName()) && potentialSch.getServiceName().equals
                  (existingSch.getServiceName())) {
                  addSch = false;
                }
              }
              if (addSch)
                potentialHosts.add(potentialSch);
            }
          }
        }
        if (potentialHosts != null && !potentialHosts.isEmpty()) {
          clientSchs.put(sc.getName(), potentialHosts);
        }
      }
    }
    LOG.info("Client hosts for reinstall : " + clientSchs.size
      ());

    for (String sc : clientSchs.keySet()) {
      Map<State, List<ServiceComponentHost>> schMap = new
        HashMap<State, List<ServiceComponentHost>>();
      schMap.put(State.INSTALLED, clientSchs.get(sc));
      changedScHosts.put(sc, schMap);
    }
  }

  private List<Stage> doStageCreation(Cluster cluster,
      Map<State, List<Service>> changedServices,
      Map<State, List<ServiceComponent>> changedComps,
      Map<String, Map<State, List<ServiceComponentHost>>> changedScHosts,
      Map<String, String> requestParameters, String requestContext, boolean runSmokeTest)
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

    // smoke test any service that goes from installed to started
    Set<String> smokeTestServices = getServicesForSmokeTests(cluster,
      changedServices, changedScHosts, runSmokeTest);

    // Re-install client only hosts to reattach changed configs on service
    // restart
    addClientSchForReinstall(cluster, changedServices, changedScHosts);

    if (!changedScHosts.isEmpty()
        || !smokeTestServices.isEmpty()) {
      long nowTimestamp = System.currentTimeMillis();
      requestId = Long.valueOf(actionManager.getNextRequestId());

      // FIXME cannot work with a single stage
      // multiple stages may be needed for reconfigure
      long stageId = 0;
      Stage stage = createNewStage(cluster, requestId.longValue(), requestContext);
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
                    || oldSchState == State.INSTALLED
                    || oldSchState == State.STOPPING) {
                  roleCommand = RoleCommand.STOP;
                  event = new ServiceComponentHostStopEvent(
                      scHost.getServiceComponentName(), scHost.getHostName(),
                      nowTimestamp);
                } else if (oldSchState == State.UPGRADING) {
                  roleCommand = RoleCommand.UPGRADE;
                  event = new ServiceComponentHostUpgradeEvent(
                      scHost.getServiceComponentName(), scHost.getHostName(),
                      nowTimestamp, scHost.getDesiredStackVersion().getStackId());
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
                    || oldSchState == State.STARTING) {
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
                    || oldSchState == State.UNINSTALLING) {
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

            // [ type -> [ key, value ] ]
            Map<String, Map<String, String>> configurations = new TreeMap<String, Map<String,String>>();
            Map<String, Map<String, String>> configTags = new HashMap<String, Map<String,String>>();

            // Do not use host component config mappings.  Instead, the rules are:
            // 1) Use the cluster desired config
            // 2) override (1) with service-specific overrides
            // 3) override (2) with host-specific overrides

            // since we are dealing with host components in this loop, get the
            // config mappings for the service this host component applies to

            for (Entry<String, DesiredConfig> entry : cluster.getDesiredConfigs().entrySet()) {
              String type = entry.getKey();
              String tag = entry.getValue().getVersion();
              // 1) start with cluster config
              Config config = cluster.getConfig(type, tag);

              if (null == config)
                continue;

              Map<String, String> props = new HashMap<String, String>(config.getProperties());
              Map<String, String> tags = new HashMap<String, String>();
              tags.put("tag", config.getVersionTag());

              // 2) apply the service overrides, if any are defined with different tags
              Service service = cluster.getService(scHost.getServiceName());
              Config svcConfig = service.getDesiredConfigs().get(type);
              if (null != svcConfig && !svcConfig.getVersionTag().equals(tag)) {
                props.putAll(svcConfig.getProperties());
              }

              // 3) apply the host overrides, if any
              Host host = clusters.getHost(scHost.getHostName());
              DesiredConfig dc = host.getDesiredConfigs(scHost.getClusterId()).get(type);
              if (null != dc) {
                Config hostConfig = cluster.getConfig(type, dc.getVersion());
                if (null != hostConfig) {
                  props.putAll(hostConfig.getProperties());
                  tags.put("host_override_tag", hostConfig.getVersionTag());
                }
              }

              configurations.put(type, props);
              configTags.put(type, tags);
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

            createHostAction(cluster, stage, scHost, configurations, configTags,
                roleCommand, requestParameters, event);
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
            .setClusterHostInfo(StageUtils.getClusterHostInfo(cluster, hostsMap, injector));
      }

      RoleGraph rg = new RoleGraph(rco);
      rg.build(stage);
      return rg.getStages();
    }

    return null;
  }

  private void persistStages(List<Stage> stages) {
    if(stages != null && stages.size() > 0) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Triggering Action Manager"
            + ", clusterName=" + stages.get(0).getClusterName()
            + ", requestId=" + stages.get(0).getRequestId()
            + ", stagesCount=" + stages.size());
      }
      actionManager.sendActions(stages);
    }
  }

  private void updateServiceStates(
      Map<State, List<Service>> changedServices,
      Map<State, List<ServiceComponent>> changedComps,
      Map<String, Map<State, List<ServiceComponentHost>>> changedScHosts) {
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
            || oldState == State.INSTALL_FAILED
            || oldState == State.UPGRADING
            || oldState == State.STOPPING
            || oldState == State.MAINTENANCE) {
          return true;
        }
        break;
      case STARTED:
        if (oldState == State.INSTALLED
            || oldState == State.STARTING
            || oldState == State.STARTED) {
          return true;
        }
        break;
      case UNINSTALLED:
        if (oldState == State.INSTALLED
            || oldState == State.UNINSTALLED
            || oldState == State.UNINSTALLING) {
          return true;
        }
      case INIT:
        if (oldState == State.UNINSTALLED
            || oldState == State.INIT
            || oldState == State.WIPING_OUT) {
          return true;
        }
      case MAINTENANCE:
        if (oldState == State.INSTALLED) {
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
            || oldState == State.STARTED
            || oldState == State.STOPPING) {
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
      Set<ServiceRequest> requests, Map<String, String> requestProperties,
      boolean runSmokeTest) throws AmbariException {

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
          Config config = cluster.getConfig(
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
          if (oldSchState == State.MAINTENANCE) {
            //Ignore host components updates in this state
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
           * This is hack for now wherein we don't fail if the
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
          Config config = cluster.getConfig(entry.getKey(), entry.getValue());
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

    List<Stage> stages = doStageCreation(cluster, changedServices, changedComps,
      changedScHosts, null, requestProperties.get(REQUEST_CONTEXT_PROPERTY),
      runSmokeTest);
    persistStages(stages);
    updateServiceStates(changedServices, changedComps, changedScHosts);
    if (stages == null || stages.isEmpty()) {
      return null;
    }

    return getRequestStatusResponse(stages.get(0).getRequestId());
  }

  @Override
  public synchronized RequestStatusResponse updateComponents(Set<ServiceComponentRequest> requests,
                                                             Map<String, String> requestProperties, boolean runSmokeTest)
                                                             throws AmbariException {

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
          Config config = cluster.getConfig(
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
        if (oldSchState == State.MAINTENANCE) {
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
          Config config = cluster.getConfig(
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

    List<Stage> stages = doStageCreation(cluster, null,
        changedComps, changedScHosts, null, requestProperties.get(REQUEST_CONTEXT_PROPERTY), runSmokeTest);
    persistStages(stages);
    updateServiceStates(null, changedComps, changedScHosts);
    if (stages == null || stages.isEmpty()) {
      return null;
    }

    return getRequestStatusResponse(stages.get(0).getRequestId());
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

      if (null != request.getClusterName() && null != request.getDesiredConfig()) {
        Cluster c = clusters.getCluster(request.getClusterName());

        if (clusters.getHostsForCluster(request.getClusterName()).containsKey(h.getHostName())) {

          ConfigurationRequest cr = request.getDesiredConfig();

          if (null != cr.getProperties() && cr.getProperties().size() > 0) {
            cr.setClusterName(c.getClusterName());
            createConfiguration(cr);
          }

          Config baseConfig = c.getConfig(cr.getType(), cr.getVersionTag());
          if (null != baseConfig)
            h.addDesiredConfig(c.getClusterId(), cr.isSelected(), baseConfig);

        }
      }

      //todo: if attempt was made to update a property other than those
      //todo: that are allowed above, should throw exception
    }
  }

  @Override
  public synchronized RequestStatusResponse updateHostComponents(Set<ServiceComponentHostRequest> requests,
                                                                 Map<String, String> requestProperties, boolean runSmokeTest)
                                                                 throws AmbariException {

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
    boolean processingUpgradeRequest = false;
    int numberOfRequestsProcessed = 0;
    StackId fromStackVersion = new StackId();
    Map<ServiceComponentHost, State> directTransitionScHosts = new HashMap<ServiceComponentHost, State>();
    for (ServiceComponentHostRequest request : requests) {
      numberOfRequestsProcessed++;
      validateServiceComponentHostRequest(request);

      Cluster cluster = clusters.getCluster(request.getClusterName());

      if (StringUtils.isEmpty(request.getServiceName())) {
        request.setServiceName(findServiceName(cluster, request.getComponentName()));
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
            new HashMap<String, Map<String, Set<String>>>());
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

        for (Entry<String, String> entry :
            request.getConfigVersions().entrySet()) {
          Config config = cluster.getConfig(
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

      // If upgrade request comes without state information then its an error
      boolean upgradeRequest = checkIfUpgradeRequestAndValidate(request, cluster, s, sc, sch);

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

      if (!processingUpgradeRequest && upgradeRequest) {
        processingUpgradeRequest = true;
        // this needs to be the first request
        if (numberOfRequestsProcessed > 1) {
          throw new AmbariException("An upgrade request cannot be combined with " +
              "other non-upgrade requests.");
        }
        fromStackVersion = sch.getStackVersion();
      }

      if (processingUpgradeRequest) {
        if (!upgradeRequest) {
          throw new AmbariException("An upgrade request cannot be combined with " +
              "other non-upgrade requests.");
        }
        sch.setState(State.UPGRADING);
        sch.setDesiredStackVersion(cluster.getCurrentStackVersion());
      }

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

      if (isDirectTransition(oldSchState, newState)) {

//        if (newState == State.DELETED) {
//          if (!sch.canBeRemoved()) {
//            throw new AmbariException("Servicecomponenthost cannot be removed"
//                + ", clusterName=" + cluster.getClusterName()
//                + ", clusterId=" + cluster.getClusterId()
//                + ", serviceName=" + sch.getServiceName()
//                + ", componentName=" + sch.getServiceComponentName()
//                + ", hostname=" + sch.getHostName()
//                + ", currentState=" + oldSchState
//                + ", newDesiredState=" + newState);
//          }
//        }

        if (LOG.isDebugEnabled()) {
          LOG.debug("Handling direct transition update to ServiceComponentHost"
              + ", clusterName=" + request.getClusterName()
              + ", serviceName=" + s.getName()
              + ", componentName=" + sc.getName()
              + ", hostname=" + sch.getHostName()
              + ", currentState=" + oldSchState
              + ", newDesiredState=" + newState);
        }
        directTransitionScHosts.put(sch, newState);
      } else {
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
    for (ServiceComponentHostRequest request : requests) {
      Cluster cluster = clusters.getCluster(request.getClusterName());
      Service s = cluster.getService(request.getServiceName());
      ServiceComponent sc = s.getServiceComponent(
          request.getComponentName());
      ServiceComponentHost sch = sc.getServiceComponentHost(
          request.getHostname());
      if (request.getConfigVersions() != null) {
        Map<String, Config> updated = new HashMap<String, Config>();

        for (Entry<String, String> entry : request.getConfigVersions().entrySet()) {
          Config config = cluster.getConfig(
              entry.getKey(), entry.getValue());
          updated.put(config.getType(), config);

          if (!updated.isEmpty()) {
            sch.updateDesiredConfigs(updated);
          }
        }
      }
    }

    // Perform direct transitions (without task generation)
    for (Entry<ServiceComponentHost, State> entry : directTransitionScHosts.entrySet()) {
      ServiceComponentHost componentHost = entry.getKey();
      State newState = entry.getValue();
      long timestamp = System.currentTimeMillis();
      ServiceComponentHostEvent event;
      componentHost.setDesiredState(newState);
      switch (newState) {
        case MAINTENANCE:
          event = new ServiceComponentHostMaintenanceEvent(
              componentHost.getServiceComponentName(),
              componentHost.getHostName(),
              timestamp);
          break;
        case INSTALLED:
          event = new ServiceComponentHostRestoreEvent(
              componentHost.getServiceComponentName(),
              componentHost.getHostName(),
              timestamp);
          break;
        default:
          throw new AmbariException("Direct transition from " + componentHost.getState() + " to " + newState + " not supported");
      }
      try {
        componentHost.handleEvent(event);
      } catch (InvalidStateTransitionException e) {
        //Should not occur, must be covered by previous checks
        throw new AmbariException("Internal error - not supported transition", e);
      }
    }

    Cluster cluster = clusters.getCluster(clusterNames.iterator().next());

    Map<String, String> requestParameters = null;
    if (processingUpgradeRequest) {
      requestParameters = new HashMap<String, String>();
      requestParameters.put(Configuration.UPGRADE_TO_STACK, gson.toJson(cluster.getCurrentStackVersion()));
      requestParameters.put(Configuration.UPGRADE_FROM_STACK, gson.toJson(fromStackVersion));
    }
    List<Stage> stages = doStageCreation(cluster, null, null, changedScHosts, requestParameters,
        requestProperties.get(REQUEST_CONTEXT_PROPERTY), runSmokeTest);
    persistStages(stages);
    updateServiceStates(null, null, changedScHosts);
    if (stages == null || stages.isEmpty()) {
      return null;
    }

    return getRequestStatusResponse(stages.get(0).getRequestId());
  }

  private void validateServiceComponentHostRequest(ServiceComponentHostRequest request) {
    if (request.getClusterName() == null
        || request.getClusterName().isEmpty()
        || request.getComponentName() == null
        || request.getComponentName().isEmpty()
        || request.getHostname() == null
        || request.getHostname().isEmpty()) {
      throw new IllegalArgumentException("Invalid arguments"
          + ", cluster name, component name and host name should be"
          + " provided");
    }
  }

  private String findServiceName(Cluster cluster, String componentName) throws AmbariException {
    StackId stackId = cluster.getDesiredStackVersion();
    String serviceName =
        ambariMetaInfo.getComponentToService(stackId.getStackName(),
            stackId.getStackVersion(), componentName);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Looking up service name for component"
          + ", componentName=" + componentName
          + ", serviceName=" + serviceName);
    }

    if (serviceName == null
        || serviceName.isEmpty()) {
      throw new AmbariException("Could not find service for component"
          + ", componentName=" + componentName
          + ", clusterName=" + cluster.getClusterName()
          + ", stackInfo=" + stackId.getStackId());
    }
    return serviceName;
  }

  private boolean isDirectTransition(State oldState, State newState) {
    switch (newState) {
      case INSTALLED:
        if (oldState == State.MAINTENANCE) {
          return true;
        }
        break;
      case MAINTENANCE:
        if (oldState == State.INSTALLED) {
          return true;
        }
        break;
    }
    return false;
  }

  private boolean checkIfUpgradeRequestAndValidate(ServiceComponentHostRequest request, Cluster cluster, Service s,
                                                   ServiceComponent sc, ServiceComponentHost sch)
      throws AmbariException {
    boolean isUpgradeRequest = false;
    String requestedStackIdString = request.getDesiredStackId();
    StackId requestedStackId;

    if (requestedStackIdString == null) {
      return isUpgradeRequest;
    }

    try {
      requestedStackId = new StackId(request.getDesiredStackId());
    } catch (RuntimeException re) {
      throw getHostComponentUpgradeException(request, cluster, s, sc, sch,
          "Invalid desired stack id");
    }

    StackId clusterStackId = cluster.getCurrentStackVersion();
    StackId currentSchStackId = sch.getStackVersion();
    if (clusterStackId == null || clusterStackId.getStackName().equals("")) {
      // cluster has not been upgraded yet
      if (requestedStackId.compareTo(currentSchStackId) != 0) {
        throw getHostComponentUpgradeException(request, cluster, s, sc, sch,
            "Cluster has not been upgraded yet, component host cannot be upgraded");
      }
    } else {
      // cluster is upgraded and sch can be independently upgraded
      if (clusterStackId.getStackName().compareTo(requestedStackId.getStackName()) != 0) {
        throw getHostComponentUpgradeException(request, cluster, s, sc, sch,
            "Deployed stack name and requested stack names do not match");
      }
      if (clusterStackId.compareTo(requestedStackId) != 0) {
        throw getHostComponentUpgradeException(request, cluster, s, sc, sch,
            "Component host can only be upgraded to the same version as the cluster");
      } else if (requestedStackId.compareTo(currentSchStackId) > 0) {
        isUpgradeRequest = true;
        if (sch.getState() != State.INSTALLED && sch.getState() != State.UPGRADING) {
          throw getHostComponentUpgradeException(request, cluster, s, sc, sch,
              "Component host is in an invalid state for upgrade");
        }
        // Ensure that the request only updates the stack id
        if (request.getConfigVersions() != null) {
          throw getHostComponentUpgradeException(request, cluster, s, sc, sch,
              "Upgrade cannot be accompanied with config modification");
        }
        if (request.getDesiredState() == null
            || !request.getDesiredState().equals(State.INSTALLED.toString())) {
          throw getHostComponentUpgradeException(request, cluster, s, sc, sch,
              "The desired state for an upgrade request must be " + State.INSTALLED);
        }
        LOG.info("Received upgrade request to " + requestedStackId + " for "
            + "component " + sch.getServiceComponentName()
            + " on " + sch.getHostName());
      } else {
        LOG.info("Stack id " + requestedStackId + " provided in the request matches"
            + " the current stack id of the "
            + "component " + sch.getServiceComponentName()
            + " on " + sch.getHostName() + ". It will not be upgraded.");
      }
    }

    return isUpgradeRequest;
  }

  private AmbariException getHostComponentUpgradeException(
      ServiceComponentHostRequest request, Cluster cluster,
      Service s, ServiceComponent sc, ServiceComponentHost sch,
      String message) throws AmbariException {
    return new AmbariException(message
        + ", clusterName=" + cluster.getClusterName()
        + ", clusterId=" + cluster.getClusterId()
        + ", serviceName=" + s.getName()
        + ", componentName=" + sc.getName()
        + ", hostname=" + sch.getHostName()
        + ", requestedStackId=" + request.getDesiredStackId()
        + ", requestedState=" + request.getDesiredState()
        + ", clusterStackId=" + cluster.getCurrentStackVersion()
        + ", hostComponentCurrentStackId=" + sch.getStackVersion());
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
  }

  @Override
  public RequestStatusResponse deleteServices(Set<ServiceRequest> request)
      throws AmbariException {

    for (ServiceRequest serviceRequest : request) {
      if (StringUtils.isEmpty(serviceRequest.getClusterName()) || StringUtils.isEmpty(serviceRequest.getServiceName())) {
        // FIXME throw correct error
        throw new AmbariException("invalid arguments");
      } else {
        clusters.getCluster(serviceRequest.getClusterName()).deleteService(serviceRequest.getServiceName());
      }
    }
    return null;

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
      Set<ServiceComponentHostRequest> requests) throws AmbariException {

    Map<ServiceComponent, Set<ServiceComponentHost>> safeToRemoveSCHs = new HashMap<ServiceComponent, Set<ServiceComponentHost>>();

    for (ServiceComponentHostRequest request : requests) {

      validateServiceComponentHostRequest(request);

      Cluster cluster = clusters.getCluster(request.getClusterName());

      if (StringUtils.isEmpty(request.getServiceName())) {
        request.setServiceName(findServiceName(cluster, request.getComponentName()));
      }

      if (LOG.isDebugEnabled()) {
        LOG.debug("Received a hostComponent DELETE request"
            + ", clusterName=" + request.getClusterName()
            + ", serviceName=" + request.getServiceName()
            + ", componentName=" + request.getComponentName()
            + ", hostname=" + request.getHostname()
            + ", request=" + request);
      }

      Service service = cluster.getService(request.getServiceName());
      ServiceComponent component = service.getServiceComponent(request.getComponentName());
      ServiceComponentHost componentHost = component.getServiceComponentHost(request.getHostname());

      if (!componentHost.canBeRemoved()) {
        throw new AmbariException("Host Component cannot be removed"
            + ", clusterName=" + request.getClusterName()
            + ", serviceName=" + request.getServiceName()
            + ", componentName=" + request.getComponentName()
            + ", hostname=" + request.getHostname()
            + ", request=" + request);
      }

      //Only allow removing master components in MAINTENANCE state without stages generation
      if (component.isClientComponent() ||
          componentHost.getState() != State.MAINTENANCE) {
        throw new AmbariException("Only master or slave component can be removed. They must be in " +
            "MAINTENANCE state in order to be removed.");
      }

      if (!safeToRemoveSCHs.containsKey(component)) {
        safeToRemoveSCHs.put(component, new HashSet<ServiceComponentHost>());
      }
      safeToRemoveSCHs.get(component).add(componentHost);
    }

    for (Entry<ServiceComponent, Set<ServiceComponentHost>> entry : safeToRemoveSCHs.entrySet()) {
      for (ServiceComponentHost componentHost : entry.getValue()) {
        entry.getKey().deleteServiceComponentHosts(componentHost.getHostName());
      }
    }

    return null;
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

  @Transactional
  Collection<RequestStatusResponse> getRequestStatusResponses(List<Long> requestIds) {
    List<HostRoleCommand> hostRoleCommands = actionManager.getAllTasksByRequestIds(requestIds);
    Map<Long, String> requestContexts = actionManager.getRequestContext(requestIds);
    Map<Long, RequestStatusResponse> responseMap = new HashMap<Long, RequestStatusResponse>();

    for (HostRoleCommand hostRoleCommand : hostRoleCommands) {
      Long requestId = hostRoleCommand.getRequestId();
      RequestStatusResponse response = responseMap.get(requestId);
      if (response == null) {
        response = new RequestStatusResponse(requestId);
        response.setRequestContext(requestContexts.get(requestId));
        response.setTasks(new ArrayList<ShortTaskStatus>());
        responseMap.put(requestId, response);
      }

      response.getTasks().add(new ShortTaskStatus(hostRoleCommand));
    }

    return responseMap.values();
  }


  private RequestStatusResponse getRequestStatusResponse(long requestId) {
    RequestStatusResponse response = new RequestStatusResponse(requestId);
    List<HostRoleCommand> hostRoleCommands =
        actionManager.getRequestTasks(requestId);

    response.setRequestContext(actionManager.getRequestContext(requestId));
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
      RequestStatus requestStatus = null;
      if (request.getRequestStatus() != null) {
        requestStatus = RequestStatus.valueOf(request.getRequestStatus());
      }
      if (LOG.isDebugEnabled()) {
        LOG.debug("Received a Get Request Status request"
            + ", requestId=null"
            + ", requestStatus=" + requestStatus);
      }
      List<Long> requestIds = actionManager.getRequestsByStatus(requestStatus);
      response.addAll(getRequestStatusResponses(requestIds));

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

    Cluster cluster = clusters.getCluster(clusterName);
    
    // [ type -> [ key, value ] ]
    Map<String, Map<String, String>> configurations = new TreeMap<String, Map<String,String>>();

    // Do not use service config mappings.  Instead, the rules are:
    // 1) Use the cluster desired config
    // 2) override (1) with service-specific overrides
    // 3) override (2) with host-specific overrides
    // Yes, we may be sending more configs than are actually used, but that is
    // because of the new design

    for (Entry<String, DesiredConfig> entry : cluster.getDesiredConfigs().entrySet()) {
      String type = entry.getKey();
      String tag = entry.getValue().getVersion();
      // 1) start with cluster config
      Config config = cluster.getConfig(type, tag);

      if (null == config)
        continue;

      Map<String, String> props = new HashMap<String, String>(config.getProperties());

      // 2) apply the service overrides, if any are defined with different tags
      Service service = cluster.getService(actionRequest.getServiceName());
      Config svcConfig = service.getDesiredConfigs().get(type);
      if (null != svcConfig && !svcConfig.getVersionTag().equals(tag)) {
        props.putAll(svcConfig.getProperties());
      }

      // 3) apply the host overrides, if any
      Host host = clusters.getHost(hostName);
      DesiredConfig dc = host.getDesiredConfigs(cluster.getClusterId()).get(type);
      if (null != dc) {
        Config hostConfig = cluster.getConfig(type, dc.getVersion());
        if (null != hostConfig) {
          props.putAll(hostConfig.getProperties());
        }
      }

      configurations.put(type, props);
    }

    ExecutionCommand execCmd = stage.getExecutionCommandWrapper(hostName,
      actionRequest.getActionName()).getExecutionCommand();

    execCmd.setConfigurations(configurations);

    Map<String, String> params = new TreeMap<String, String>();
    params.put("jdk_location", this.jdkResourceUrl);
    params.put("stack_version", cluster.getDesiredStackVersion().getStackVersion());
    execCmd.setHostLevelParams(params);

    // Generate cluster host info
    execCmd.setClusterHostInfo(
      StageUtils.getClusterHostInfo(clusters.getCluster(clusterName), hostsMap, injector));
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

    Config config = clusters.getCluster(clusterName).getConfig(
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

    ExecutionCommand execCmd = stage.getExecutionCommandWrapper(namenodeHost,
      Role.DECOMMISSION_DATANODE.toString()).getExecutionCommand();

    execCmd.setConfigurations(configurations);

    Cluster cluster = clusters.getCluster(clusterName);
    Map<String, String> params = new TreeMap<String, String>();
    params.put("jdk_location", this.jdkResourceUrl);
    params.put("stack_version", cluster.getDesiredStackVersion()
      .getStackVersion());
    execCmd.setHostLevelParams(params);

  }

  @Override
  public RequestStatusResponse createActions(Set<ActionRequest> request, Map<String, String> requestProperties)
      throws AmbariException {
    String clusterName = null;

    String requestContext = "";
    
    if (requestProperties != null)
      requestContext = requestProperties.get(REQUEST_CONTEXT_PROPERTY);
      
    
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
        logDir, clusterName, requestContext);
    
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


  @Override
  public Set<StackResponse> getStacks(Set<StackRequest> requests)
      throws AmbariException {
    Set<StackResponse> response = new HashSet<StackResponse>();
    for (StackRequest request : requests) {
      try {
        response.addAll(getStacks(request));
      } catch (StackAccessException e) {
        if (requests.size() == 1) {
          // only throw exception if 1 request.
          // there will be > 1 request in case of OR predicate
          throw e;
        }
      }
    }
    return response;

  }

  private Set<StackResponse> getStacks(StackRequest request)
      throws AmbariException {
    Set<StackResponse> response;

    String stackName = request.getStackName();

    if (stackName != null) {
      org.apache.ambari.server.state.Stack stack = this.ambariMetaInfo.getStack(stackName);
      response = Collections.singleton(stack.convertToResponse());
    } else {
      Set<org.apache.ambari.server.state.Stack> supportedStackNames = this.ambariMetaInfo.getStackNames();
      response = new HashSet<StackResponse>();
      for (org.apache.ambari.server.state.Stack stack: supportedStackNames) {
        response.add(stack.convertToResponse());
      }
    }
    return response;
  }

  @Override
  public Set<RepositoryResponse> getRepositories(Set<RepositoryRequest> requests)
      throws AmbariException {
    Set<RepositoryResponse> response = new HashSet<RepositoryResponse>();
    for (RepositoryRequest request : requests) {
      try {
        response.addAll(getRepositories(request));
      } catch (StackAccessException e) {
        if (requests.size() == 1) {
          // only throw exception if 1 request.
          // there will be > 1 request in case of OR predicate
          throw e;
        }
      }
    }
    return response;
  }

  private Set<RepositoryResponse> getRepositories(RepositoryRequest request) throws AmbariException {

    String stackName = request.getStackName();
    String stackVersion = request.getStackVersion();
    String osType = request.getOsType();
    String repoId = request.getRepoId();

    Set<RepositoryResponse> response;

    if (repoId == null) {
      List<RepositoryInfo> repositories = this.ambariMetaInfo.getRepositories(stackName, stackVersion, osType);
      response = new HashSet<RepositoryResponse>();

      for (RepositoryInfo repository: repositories) {
        response.add(repository.convertToResponse());
      }

    } else {
      RepositoryInfo repository = this.ambariMetaInfo.getRepository(stackName, stackVersion, osType, repoId);
      response = Collections.singleton(repository.convertToResponse());
    }

    return response;
  }

  @Override
  public Set<StackVersionResponse> getStackVersions(
      Set<StackVersionRequest> requests) throws AmbariException {
    Set<StackVersionResponse> response = new HashSet<StackVersionResponse>();
    for (StackVersionRequest request : requests) {
      try {
        response.addAll(getStackVersions(request));
      } catch (StackAccessException e) {
        if (requests.size() == 1) {
          // only throw exception if 1 request.
          // there will be > 1 request in case of OR predicate
          throw e;
        }
      }
    }

    return response;

  }

  private Set<StackVersionResponse> getStackVersions(StackVersionRequest request) throws AmbariException {
    Set<StackVersionResponse> response = null;

    String stackName = request.getStackName();
    String stackVersion = request.getStackVersion();

    if (stackVersion != null) {
      StackInfo stackInfo = this.ambariMetaInfo.getStackInfo(stackName, stackVersion);
      response = Collections.singleton(stackInfo.convertToResponse());
    } else {
      Set<StackInfo> stackInfos = this.ambariMetaInfo.getStackInfos(stackName);
      response = new HashSet<StackVersionResponse>();
      for (StackInfo stackInfo: stackInfos) {
        response.add(stackInfo.convertToResponse());
      }
    }

    return response;
  }

  @Override
  public Set<StackServiceResponse> getStackServices(
      Set<StackServiceRequest> requests) throws AmbariException {

    Set<StackServiceResponse> response = new HashSet<StackServiceResponse>();

    for (StackServiceRequest request : requests) {
      try {
        response.addAll(getStackServices(request));
      } catch (StackAccessException e) {
        if (requests.size() == 1) {
          // only throw exception if 1 request.
          // there will be > 1 request in case of OR predicate
          throw e;
        }
      }
    }

    return response;
  }

  private Set<StackServiceResponse> getStackServices(StackServiceRequest request) throws AmbariException {
    Set<StackServiceResponse> response = null;

    String stackName = request.getStackName();
    String stackVersion = request.getStackVersion();
    String serviceName = request.getServiceName();

    if (serviceName != null) {
      ServiceInfo service = this.ambariMetaInfo.getService(stackName, stackVersion, serviceName);
      response = Collections.singleton(service.convertToResponse());
    } else {
      Map<String, ServiceInfo> services = this.ambariMetaInfo.getServices(stackName, stackVersion);
      response = new HashSet<StackServiceResponse>();
      for (ServiceInfo service : services.values()) {
        response.add(service.convertToResponse());
      }
    }
    return response;
  }

  @Override
  public Set<StackConfigurationResponse> getStackConfigurations(
      Set<StackConfigurationRequest> requests) throws AmbariException {
    Set<StackConfigurationResponse> response = new HashSet<StackConfigurationResponse>();
    for (StackConfigurationRequest request : requests) {
      response.addAll(getStackConfigurations(request));
    }

    return response;
  }

  private Set<StackConfigurationResponse> getStackConfigurations(
      StackConfigurationRequest request) throws AmbariException {

    Set<StackConfigurationResponse> response = null;

    String stackName = request.getStackName();
    String stackVersion = request.getStackVersion();
    String serviceName = request.getServiceName();
    String propertyName = request.getPropertyName();

    if (propertyName != null) {
      PropertyInfo property = this.ambariMetaInfo.getProperty(stackName, stackVersion, serviceName, propertyName);
      response = Collections.singleton(property.convertToResponse());
    } else {

      Set<PropertyInfo> properties = this.ambariMetaInfo.getProperties(stackName, stackVersion, serviceName);
      response = new HashSet<StackConfigurationResponse>();

      for (PropertyInfo property: properties) {
        response.add(property.convertToResponse());
      }
    }

    return response;
  }

  @Override
  public Set<StackServiceComponentResponse> getStackComponents(
      Set<StackServiceComponentRequest> requests) throws AmbariException {
    Set<StackServiceComponentResponse> response = new HashSet<StackServiceComponentResponse>();
    for (StackServiceComponentRequest request : requests) {
      try {
        response.addAll(getStackComponents(request));
      } catch (StackAccessException e) {
        if (requests.size() == 1) {
          // only throw exception if 1 request.
          // there will be > 1 request in case of OR predicate
          throw e;
        }
      }
    }

    return response;
  }

  private Set<StackServiceComponentResponse> getStackComponents(
      StackServiceComponentRequest request) throws AmbariException {
    Set<StackServiceComponentResponse> response = null;

    String stackName = request.getStackName();
    String stackVersion = request.getStackVersion();
    String serviceName = request.getServiceName();
    String componentName = request.getComponentName();


    if (componentName != null) {
      ComponentInfo component = this.ambariMetaInfo.getComponent(stackName, stackVersion, serviceName, componentName);
      response = Collections.singleton(component.convertToResponse());

    } else {
      List<ComponentInfo> components = this.ambariMetaInfo.getComponentsByService(stackName, stackVersion, serviceName);
      response = new HashSet<StackServiceComponentResponse>();

      for (ComponentInfo component: components) {
        response.add(component.convertToResponse());
      }
    }
    return response;
  }

  @Override
  public Set<OperatingSystemResponse> getStackOperatingSystems(
      Set<OperatingSystemRequest> requests) throws AmbariException {
    Set<OperatingSystemResponse> response = new HashSet<OperatingSystemResponse>();
    for (OperatingSystemRequest request : requests) {
      try {
        response.addAll(getStackOperatingSystems(request));
      } catch (StackAccessException e) {
        if (requests.size() == 1) {
          // only throw exception if 1 request.
          // there will be > 1 request in case of OR predicate
          throw e;
        }
      }
    }
    return response;
  }

  private Set<OperatingSystemResponse> getStackOperatingSystems(
      OperatingSystemRequest request) throws AmbariException {

    Set<OperatingSystemResponse> response = null;

    String stackName = request.getStackName();
    String stackVersion = request.getStackVersion();
    String osType = request.getOsType();

    if (osType != null) {
      OperatingSystemInfo operatingSystem = this.ambariMetaInfo.getOperatingSystem(stackName, stackVersion, osType);
      response = Collections.singleton(operatingSystem.convertToResponse());
    } else {
      Set<OperatingSystemInfo> operatingSystems = this.ambariMetaInfo.getOperatingSystems(stackName, stackVersion);
      response = new HashSet<OperatingSystemResponse>();
      for (OperatingSystemInfo operatingSystem : operatingSystems)
        response.add(operatingSystem.convertToResponse());
    }

    return response;
  }
}
