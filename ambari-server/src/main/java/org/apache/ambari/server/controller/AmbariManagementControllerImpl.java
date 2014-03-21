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

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;
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
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.RequestStageContainer;
import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.apache.ambari.server.customactions.ActionDefinition;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.metadata.RoleCommandOrder;
import org.apache.ambari.server.scheduler.ExecutionScheduleManager;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.User;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.stageplanner.RoleGraph;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.CommandScriptDefinition;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.MaintenanceState;
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
import org.apache.ambari.server.state.ServiceOsSpecific;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.configgroup.ConfigGroupFactory;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.scheduler.RequestExecutionFactory;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostDisableEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostInstallEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostRestoreEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStartEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStopEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostUpgradeEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.AMBARI_DB_RCA_DRIVER;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.AMBARI_DB_RCA_PASSWORD;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.AMBARI_DB_RCA_URL;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.AMBARI_DB_RCA_USERNAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.COMMAND_TIMEOUT;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.DB_DRIVER_FILENAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.DB_NAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.HOOKS_FOLDER;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.JAVA_HOME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.JCE_NAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.JDK_LOCATION;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.JDK_NAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.MYSQL_JDBC_URL;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.ORACLE_JDBC_URL;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.PACKAGE_LIST;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.REPO_INFO;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SCHEMA_VERSION;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SCRIPT;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SCRIPT_TYPE;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SERVICE_PACKAGE_FOLDER;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SERVICE_REPO_INFO;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.STACK_NAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.STACK_VERSION;

@Singleton
public class AmbariManagementControllerImpl implements AmbariManagementController {

  private final static Logger LOG =
      LoggerFactory.getLogger(AmbariManagementControllerImpl.class);

  /**
   * Property name of request context.
   */
  private static final String REQUEST_CONTEXT_PROPERTY = "context";

  private static final String BASE_LOG_DIR = "/tmp/ambari";

  private final Clusters clusters;

  private final ActionManager actionManager;

  @SuppressWarnings("unused")
  private final Injector injector;

  private final Gson gson;

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
  private RequestFactory requestFactory;
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
  @Inject
  private AbstractRootServiceResponseFactory rootServiceResponseFactory;
  @Inject
  private ConfigGroupFactory configGroupFactory;
  @Inject
  private ConfigHelper configHelper;
  @Inject
  private RequestExecutionFactory requestExecutionFactory;
  @Inject
  private ExecutionScheduleManager executionScheduleManager;

  private MaintenanceStateHelper maintenanceStateHelper;

  final private String masterHostname;
  final private Integer masterPort;
  final private String masterProtocol;

  final private static String JDK_RESOURCE_LOCATION =
      "/resources/";
  
  final private static int REPO_URL_CONNECT_TIMEOUT = 3000;
  final private static int REPO_URL_READ_TIMEOUT = 500;

  final private String jdkResourceUrl;
  final private String javaHome;
  final private String jdkName;
  final private String jceName;
  final private String ojdbcUrl;
  final private String serverDB;
  final private String mysqljdbcUrl;

  @Inject
  private AmbariCustomCommandExecutionHelper customCommandExecutionHelper;
  @Inject
  private AmbariActionExecutionHelper actionExecutionHelper;

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
    this.maintenanceStateHelper = injector.getInstance(MaintenanceStateHelper.class);

    if(configs != null)
    {
      if (configs.getApiSSLAuthentication()) {
        this.masterProtocol = "https";
        this.masterPort = configs.getClientSSLApiPort();
      } else {
        this.masterProtocol = "http";
        this.masterPort = configs.getClientApiPort();
      }  
      this.jdkResourceUrl = getAmbariServerURI(JDK_RESOURCE_LOCATION);
      this.javaHome = configs.getJavaHome();
      this.jdkName = configs.getJDKName();
      this.jceName = configs.getJCEName();
      this.ojdbcUrl = getAmbariServerURI(JDK_RESOURCE_LOCATION + "/" + configs.getOjdbcJarName());
      this.mysqljdbcUrl = getAmbariServerURI(JDK_RESOURCE_LOCATION + "/" + configs.getMySQLJarName());
     
      this.serverDB = configs.getServerDBName();
    } else {
      this.masterProtocol = null;
      this.masterPort = null;
      
      this.jdkResourceUrl = null;
      this.javaHome = null;
      this.jdkName = null;
      this.jceName = null;
      this.ojdbcUrl = null;
      this.mysqljdbcUrl = null;
      this.serverDB = null;
    }
  }
  
  public String getAmbariServerURI(String path) {
    if(masterProtocol==null || masterHostname==null || masterPort==null)
      return null;
    
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setScheme(masterProtocol);
    uriBuilder.setHost(masterHostname);
    uriBuilder.setPort(masterPort);
    uriBuilder.setPath(path);
    
    return uriBuilder.toString();
  }

  private RoleCommandOrder getRoleCommandOrder(Cluster cluster) {
      RoleCommandOrder rco;
      rco = injector.getInstance(RoleCommandOrder.class);
      rco.initialize(cluster);
      return rco;
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
    persistServiceComponentHosts(requests);
  }

  @Transactional
  void persistServiceComponentHosts(Set<ServiceComponentHostRequest> requests) throws AmbariException {
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
          serviceComponentHostFactory.createNew(sc, request.getHostname());

      if (request.getDesiredState() != null
          && !request.getDesiredState().isEmpty()) {
        State state = State.valueOf(request.getDesiredState());
        sch.setDesiredState(state);
      }

      sch.setDesiredStackVersion(sc.getDesiredStackVersion());

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
        || null == request.getProperties()) {
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

    if (configs.containsKey(request.getVersionTag())) {
      throw new AmbariException(MessageFormat.format("Configuration with tag ''{0}'' exists for ''{1}''",
          request.getVersionTag(),
          request.getType()));
    }

    Config config = configFactory.createNew (cluster, request.getType(),
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

  private Stage createNewStage(long id, Cluster cluster, long requestId, String requestContext, String clusterHostInfo) {
    String logDir = BASE_LOG_DIR + File.pathSeparator + requestId;
    Stage stage =
        stageFactory.createNew(requestId, logDir, cluster.getClusterName(), requestContext, clusterHostInfo);
    stage.setStageId(id);
    return stage;
  }


  private Set<ClusterResponse> getClusters(ClusterRequest request)
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
      response.add(cr);
      return response;
    } else if (request.getClusterId() != null) {
      Cluster c = clusters.getClusterById(request.getClusterId());
      ClusterResponse cr = c.convertToResponse();
      cr.setDesiredConfigs(c.getDesiredConfigs());
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

  private Set<ServiceComponentHostResponse> getHostComponents(
      ServiceComponentHostRequest request) throws AmbariException {
    LOG.debug("Processing request {}", request);

    if (request.getClusterName() == null
        || request.getClusterName().isEmpty()) {
      IllegalArgumentException e = new IllegalArgumentException("Invalid arguments, cluster name should not be null");
      LOG.debug("Cluster not specified in request", e);
      throw e;
    }

    final Cluster cluster;
    try {
      cluster = clusters.getCluster(request.getClusterName());
    } catch (ClusterNotFoundException e) {
      LOG.error("Cluster not found ", e);
      throw new ParentObjectNotFoundException("Parent Cluster resource doesn't exist", e);
    }

    if (request.getHostname() != null) {
      try {
        if (!clusters.getClustersForHost(request.getHostname()).contains(cluster)) {
          // case where host exists but not associated with given cluster
          LOG.error("Host doesn't belong to cluster");
          throw new ParentObjectNotFoundException("Parent Host resource doesn't exist",
              new HostNotFoundException(request.getClusterName(), request.getHostname()));
        }
      } catch (HostNotFoundException e) {
        LOG.error("Host not found", e);
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
          LOG.error("Unable to find service for component {}", request.getComponentName());
          throw new ServiceComponentHostNotFoundException(
              cluster.getClusterName(), null, request.getComponentName(), request.getHostname());
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
    boolean filterBasedConfigStaleness = false;
    boolean staleConfig = true;
    if (request.getStaleConfig() != null) {
      filterBasedConfigStaleness = true;
      staleConfig = "true".equals(request.getStaleConfig().toLowerCase());
    }
    if (request.getDesiredState() != null
        && !request.getDesiredState().isEmpty()) {
      desiredStateToCheck = State.valueOf(request.getDesiredState());
      if (!desiredStateToCheck.isValidDesiredState()) {
        throw new IllegalArgumentException("Invalid arguments, invalid desired"
            + " state, desiredState=" + desiredStateToCheck);
      }
      checkDesiredState = true;
    }

    Map<String, Host> hosts = clusters.getHostsForCluster(cluster.getClusterName());

    for (Service s : services) {
      // filter on component name if provided
      Set<ServiceComponent> components = new HashSet<ServiceComponent>();
      if (request.getComponentName() != null) {
        components.add(s.getServiceComponent(request.getComponentName()));
      } else {
        components.addAll(s.getServiceComponents().values());
      }
      for (ServiceComponent sc : components) {
        if (request.getComponentName() != null) {
          if (!sc.getName().equals(request.getComponentName())) {
            continue;
          }
        }

        // filter on hostname if provided
        // filter on desired state if provided

        Map<String, ServiceComponentHost> serviceComponentHostMap =
          sc.getServiceComponentHosts();

        if (request.getHostname() != null) {
          try {
            if (serviceComponentHostMap == null
                || !serviceComponentHostMap.containsKey(request.getHostname())) {
              ServiceComponentHostNotFoundException e = new ServiceComponentHostNotFoundException(cluster.getClusterName(),
                s.getName(), sc.getName(), request.getHostname());
              throw e;
            }

            ServiceComponentHost sch = serviceComponentHostMap.get(request.getHostname());

            if (checkDesiredState && (desiredStateToCheck != sch.getDesiredState())) {
              continue;
            }
            if (request.getAdminState() != null) {
              String stringToMatch =
                  sch.getComponentAdminState() == null ? "" : sch.getComponentAdminState().name();
              if (!request.getAdminState().equals(stringToMatch)) {
                continue;
              }
            }

            ServiceComponentHostResponse r = sch.convertToResponse();
            if (filterBasedConfigStaleness && r.isStaleConfig() != staleConfig) {
              continue;
            }

            Host host = hosts.get(sch.getHostName());
            if (host == null) {
              throw new HostNotFoundException(cluster.getClusterName(), sch.getHostName());
            }

            r.setMaintenanceState(maintenanceStateHelper.getEffectiveState(sch, host).name());
            response.add(r);
          } catch (ServiceComponentHostNotFoundException e) {
            if (request.getServiceName() != null && request.getComponentName() != null) {
              LOG.error("ServiceComponentHost not found ", e);
              throw new ServiceComponentHostNotFoundException(cluster.getClusterName(),
                  request.getServiceName(), request.getComponentName(), request.getHostname());
            } else {
              LOG.debug("Ignoring not specified host_component ", e);
              // ignore this since host_component was not specified
              // this is an artifact of how we get host_components and can happen
              // in case where we get all host_components for a host
            }
          }
        } else {
          for (ServiceComponentHost sch : serviceComponentHostMap.values()) {
            if (checkDesiredState && (desiredStateToCheck != sch.getDesiredState())) {
              continue;
            }

            if (request.getAdminState() != null) {
              String stringToMatch =
                  sch.getComponentAdminState() == null ? "" : sch.getComponentAdminState().name();
              if (!request.getAdminState().equals(stringToMatch)) {
                continue;
              }
            }

            ServiceComponentHostResponse r = sch.convertToResponse();
            if (filterBasedConfigStaleness && r.isStaleConfig() != staleConfig) {
              continue;
            }

            Host host = hosts.get(sch.getHostName());
            if (host == null) {
              throw new HostNotFoundException(cluster.getClusterName(), sch.getHostName());
            }
            
            r.setMaintenanceState(maintenanceStateHelper.getEffectiveState(sch, host).name());
            response.add(r);
          }
        }
      }
    }
    return response;
  }
  
  @Override
  public MaintenanceState getEffectiveMaintenanceState(ServiceComponentHost sch)
      throws AmbariException {
    
    return maintenanceStateHelper.getEffectiveState(sch);
  }
  

  private Set<ConfigurationResponse> getConfigurations(
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
  public synchronized RequestStatusResponse updateClusters(Set<ClusterRequest> requests,
                                                           Map<String, String> requestProperties)
      throws AmbariException {

    RequestStatusResponse response = null;

    // We have to allow for multiple requests to account for multiple
    // configuration updates (create multiple configuration resources)...
    for (ClusterRequest request : requests) {
      // TODO : Is there ever a real world case where we could have multiple non-null responses?
      response = updateCluster(request);
    }
    return response;
  }

  private synchronized RequestStatusResponse updateCluster(ClusterRequest request)
      throws AmbariException {

    if (request.getClusterName() == null
        || request.getClusterName().isEmpty()) {
      throw new IllegalArgumentException("Invalid arguments, cluster name"
          + " should not be null");
    }

    LOG.info("Received a updateCluster request"
        + ", clusterName=" + request.getClusterName()
        + ", request=" + request);

    final Cluster cluster = clusters.getCluster(request.getClusterName());

    // set or create configuration mapping (and optionally create the map of properties)
    if (null != request.getDesiredConfig()) {
      ConfigurationRequest cr = request.getDesiredConfig();

      Config oldConfig = cluster.getDesiredConfigByType(cr.getType());
      
      if (null != cr.getProperties()) { 
        LOG.info(MessageFormat.format("Applying configuration with tag ''{0}'' to cluster ''{1}''",
            cr.getVersionTag(),
            request.getClusterName()));

        cr.setClusterName(cluster.getClusterName());
        createConfiguration(cr);
      }

      Config baseConfig = cluster.getConfig(cr.getType(), cr.getVersionTag());
      if (null != baseConfig) {
        String authName = getAuthName();
        
        if (cluster.addDesiredConfig(authName, baseConfig)) {
          Logger logger = LoggerFactory.getLogger("configchange");
          logger.info("cluster '" + request.getClusterName() + "' "
              + "changed by: '" + authName + "'; "
              + "type='" + baseConfig.getType() + "' "
              + "tag='" + baseConfig.getVersionTag() + "'"
              + (null == oldConfig ? "" : " from='"+ oldConfig.getVersionTag() + "'"));
        }
      }
    }

    StackId currentVersion = cluster.getCurrentStackVersion();
    StackId desiredVersion = cluster.getDesiredStackVersion();

    // Set the current version value if its not already set
    if (currentVersion == null) {
      cluster.setCurrentStackVersion(desiredVersion);
    }

    boolean requiresHostListUpdate =
        request.getHostNames() != null && !request.getHostNames().isEmpty();

    if (requiresHostListUpdate) {
      clusters.mapHostsToCluster(
          request.getHostNames(), request.getClusterName());
    }

    return null;
  }

  public String getJobTrackerHost(Cluster cluster) {
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

    // This is done to account for services with client only components.
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

    // Flatten changed Schs that are going to be Started
    List<ServiceComponentHost> serviceComponentHosts = new ArrayList<ServiceComponentHost>();
    if (changedScHosts != null && !changedScHosts.isEmpty()) {
      for (String sc : changedScHosts.keySet()) {
        for (State state : changedScHosts.get(sc).keySet()) {
          if (state == State.STARTED) {
            serviceComponentHosts.addAll(changedScHosts.get(sc).get(state));
          }
        }
      }
    }

    if (!serviceComponentHosts.isEmpty()) {
      for (ServiceComponentHost sch : serviceComponentHosts) {
        services.add(sch.getServiceName());
      }
    }

    if (services.isEmpty()) {
      return;
    }

    Map<String, List<ServiceComponentHost>> clientSchs = new HashMap<String, List<ServiceComponentHost>>();

    for (String serviceName : services) {
      Service s = cluster.getService(serviceName);
      for (String component : s.getServiceComponents().keySet()) {
        List<ServiceComponentHost> potentialHosts = new ArrayList<ServiceComponentHost>();
        ServiceComponent sc = s.getServiceComponents().get(component);
        if (sc.isClientComponent()) {
          for (ServiceComponentHost potentialSch : sc.getServiceComponentHosts().values()) {
            Host host = clusters.getHost(potentialSch.getHostName());
            // Host is alive and neither host nor SCH is in Maintenance State
            if (!potentialSch.getHostState().equals(HostState.HEARTBEAT_LOST)
                && potentialSch.getMaintenanceState() != MaintenanceState.ON
                && host.getMaintenanceState(cluster.getClusterId()) == MaintenanceState.OFF) {
              potentialHosts.add(potentialSch);
            }
          }
        }
        if (!potentialHosts.isEmpty()) {
          clientSchs.put(sc.getName(), potentialHosts);
        }
      }
    }
    LOG.info("Client hosts for reinstall : " + clientSchs.size());

    if (changedScHosts != null) {
      for (String sc : clientSchs.keySet()) {
        Map<State, List<ServiceComponentHost>> schMap = new HashMap<State, List<ServiceComponentHost>>();
        schMap.put(State.INSTALLED, clientSchs.get(sc));
        changedScHosts.put(sc, schMap);
      }
    }
  }

  @Override
  public Map<String, Map<String,String>> findConfigurationTagsWithOverrides(
          Cluster cluster, String hostName) throws AmbariException {

    return configHelper.getEffectiveDesiredTags(cluster, hostName);
  }

  @Override
  public RequestExecutionFactory getRequestExecutionFactory() {
    return requestExecutionFactory;
  }

  @Override
  public ExecutionScheduleManager getExecutionScheduleManager() {
    return executionScheduleManager;
  }

  /**
   * Creates and populates an EXECUTION_COMMAND for host
   */
  private void createHostAction(Cluster cluster,
                                Stage stage, ServiceComponentHost scHost,
                                Map<String, Map<String, String>> configurations,
                                Map<String, Map<String, String>> configTags,
                                RoleCommand roleCommand,
                                Map<String, String> commandParams,
                                ServiceComponentHostEvent event
                                )
                                throws AmbariException {

    stage.addHostRoleExecutionCommand(scHost.getHostName(), Role.valueOf(scHost
      .getServiceComponentName()), roleCommand,
      event, scHost.getClusterName(),
      scHost.getServiceName());
    String serviceName = scHost.getServiceName();
    String componentName = event.getServiceComponentName();
    String hostname = scHost.getHostName();
    String osType = clusters.getHost(hostname).getOsType();
    StackId stackId = cluster.getDesiredStackVersion();
    ServiceInfo serviceInfo = ambariMetaInfo.getServiceInfo(stackId.getStackName(),
      stackId.getStackVersion(), serviceName);
    ComponentInfo componentInfo = ambariMetaInfo.getComponent(
      stackId.getStackName(), stackId.getStackVersion(),
      serviceName, componentName);
    StackInfo stackInfo = ambariMetaInfo.getStackInfo(stackId.getStackName(),
      stackId.getStackVersion());

    ExecutionCommand execCmd = stage.getExecutionCommandWrapper(scHost.getHostName(),
      scHost.getServiceComponentName()).getExecutionCommand();

    Host host = clusters.getHost(scHost.getHostName());

    // Hack - Remove passwords from configs
    if (event.getServiceComponentName().equals(Role.HIVE_CLIENT.toString())) {
      configHelper.applyCustomConfig(configurations, Configuration.HIVE_CONFIG_TAG,
        Configuration.HIVE_METASTORE_PASSWORD_PROPERTY, "", true);
    }

    String jobtrackerHost = getJobTrackerHost(cluster);
    if (!scHost.getHostName().equals(jobtrackerHost)) {
      if (configTags.get(Configuration.GLOBAL_CONFIG_TAG) != null) {
        configHelper.applyCustomConfig(
          configurations, Configuration.GLOBAL_CONFIG_TAG,
          Configuration.RCA_ENABLED_PROPERTY, "false", false);
      }
    }

    execCmd.setConfigurations(configurations);
    execCmd.setConfigurationTags(configTags);
    if (commandParams == null) { // if not defined
      commandParams = new TreeMap<String, String>();
    }
    commandParams.put(SCHEMA_VERSION, serviceInfo.getSchemaVersion());


    // Get command script info for custom command/custom action
    /*
     * TODO: Custom actions are not supported yet, that's why we just pass
     * component main commandScript to agent. This script is only used for
     * default commads like INSTALL/STOP/START/CONFIGURE
     */
    String commandTimeout = configs.getDefaultAgentTaskTimeout();
    CommandScriptDefinition script = componentInfo.getCommandScript();
    if (serviceInfo.getSchemaVersion().equals(AmbariMetaInfo.SCHEMA_VERSION_2)) {
      if (script != null) {
        commandParams.put(SCRIPT, script.getScript());
        commandParams.put(SCRIPT_TYPE, script.getScriptType().toString());
        if (script.getTimeout() > 0) {
          commandTimeout = String.valueOf(script.getTimeout());
        }
      } else {
        String message = String.format("Component %s of service %s has no " +
          "command script defined", componentName, serviceName);
        throw new AmbariException(message);
      }
    }
    commandParams.put(COMMAND_TIMEOUT, commandTimeout);
    commandParams.put(SERVICE_PACKAGE_FOLDER,
      serviceInfo.getServicePackageFolder());
    commandParams.put(HOOKS_FOLDER, stackInfo.getStackHooksFolder());

    execCmd.setCommandParams(commandParams);

    String repoInfo = customCommandExecutionHelper.getRepoInfo(cluster, host);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Sending repo information to agent"
        + ", hostname=" + scHost.getHostName()
        + ", clusterName=" + cluster.getClusterName()
        + ", stackInfo=" + stackId.getStackId()
        + ", repoInfo=" + repoInfo);
    }

    Map<String, String> hostParams = new TreeMap<String, String>();
    hostParams.put(REPO_INFO, repoInfo);
    hostParams.put(JDK_LOCATION, getJdkResourceUrl());
    hostParams.put(JAVA_HOME, getJavaHome());
    hostParams.put(JDK_NAME, getJDKName());
    hostParams.put(JCE_NAME, getJCEName());
    hostParams.put(STACK_NAME, stackId.getStackName());
    hostParams.put(STACK_VERSION, stackId.getStackVersion());
    hostParams.put(DB_NAME, getServerDB());
    hostParams.put(MYSQL_JDBC_URL, getMysqljdbcUrl());
    hostParams.put(ORACLE_JDBC_URL, getOjdbcUrl());
    hostParams.putAll(getRcaParameters());

    // Write down os specific info for the service
    ServiceOsSpecific anyOs = null;
    if (serviceInfo.getOsSpecifics().containsKey(AmbariMetaInfo.ANY_OS)) {
      anyOs = serviceInfo.getOsSpecifics().get(AmbariMetaInfo.ANY_OS);
    }
    ServiceOsSpecific hostOs = null;
    if (serviceInfo.getOsSpecifics().containsKey(osType)) {
      hostOs = serviceInfo.getOsSpecifics().get(osType);
      // Choose repo that is relevant for host
      ServiceOsSpecific.Repo serviceRepo = hostOs.getRepo();
      if (serviceRepo != null) {
        String serviceRepoInfo = gson.toJson(serviceRepo);
        hostParams.put(SERVICE_REPO_INFO, serviceRepoInfo);
      }
    }
    // Build package list that is relevant for host
    List<ServiceOsSpecific.Package> packages =
      new ArrayList<ServiceOsSpecific.Package>();
    if (anyOs != null) {
      packages.addAll(anyOs.getPackages());
    }

    if (hostOs != null) {
      packages.addAll(hostOs.getPackages());
    }
    String packageList = gson.toJson(packages);
    hostParams.put(PACKAGE_LIST, packageList);

    if (configs.getServerDBName().equalsIgnoreCase(Configuration
      .ORACLE_DB_NAME)) {
      hostParams.put(DB_DRIVER_FILENAME, configs.getOjdbcJarName());
    } else if (configs.getServerDBName().equalsIgnoreCase(Configuration
      .MYSQL_DB_NAME)) {
      hostParams.put(DB_DRIVER_FILENAME, configs.getMySQLJarName());
    }
    execCmd.setHostLevelParams(hostParams);

    Map<String, String> roleParams = new TreeMap<String, String>();
    execCmd.setRoleParams(roleParams);

    // Send passive host info to the Nagios host role
    if (execCmd.getRole().equals(Role.NAGIOS_SERVER.name())) {
      execCmd.setPassiveInfo(
        MaintenanceStateHelper.getMaintenanceHostComponents(clusters, cluster));
    }
  }

  private ActionExecutionContext getActionExecutionContext
      (ExecuteActionRequest actionRequest) throws AmbariException {

    if (actionRequest.isCommand()) {
      return new ActionExecutionContext(actionRequest.getClusterName(),
        actionRequest.getCommandName(), actionRequest.getResourceFilters(),
        actionRequest.getParameters());
    } else {

    ActionDefinition actionDef = ambariMetaInfo.getActionDefinition(actionRequest.getActionName());

    if (actionDef == null) {
      throw new AmbariException("Action " + actionRequest.getActionName() + " does not exist");
    }

    return new ActionExecutionContext(actionRequest.getClusterName(),
      actionRequest.getActionName(), actionRequest.getResourceFilters(),
      actionRequest.getParameters(), actionDef.getTargetType(),
      actionDef.getDefaultTimeout(), actionDef.getTargetService(),
      actionDef.getTargetComponent());

    }
  }

  private List<Stage> doStageCreation(RequestStageContainer requestStages,
      Cluster cluster,
      Map<State, List<Service>> changedServices,
      Map<State, List<ServiceComponent>> changedComps,
      Map<String, Map<State, List<ServiceComponentHost>>> changedScHosts,
      Map<String, String> requestParameters, String requestContext,
      boolean runSmokeTest, boolean reconfigureClients)
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

    // smoke test any service that goes from installed to started
    Set<String> smokeTestServices = getServicesForSmokeTests(cluster,
      changedServices, changedScHosts, runSmokeTest);

    if (reconfigureClients) {
      // Re-install client only hosts to reattach changed configs on service
      // restart
      addClientSchForReinstall(cluster, changedServices, changedScHosts);
    }

    if (!changedScHosts.isEmpty()
        || !smokeTestServices.isEmpty()) {
      long nowTimestamp = System.currentTimeMillis();

      // FIXME cannot work with a single stage
      // multiple stages may be needed for reconfigure
      Map<String, Set<String>> clusterHostInfo = StageUtils.getClusterHostInfo(
          clusters.getHostsForCluster(cluster.getClusterName()), cluster);

      String clusterHostInfoJson = StageUtils.getGson().toJson(clusterHostInfo);

      Stage stage = createNewStage(requestStages.getLastStageId() + 1, cluster,
          requestStages.getId(), requestContext, clusterHostInfoJson);

      //HACK
      String jobtrackerHost = getJobTrackerHost(cluster);
      for (String compName : changedScHosts.keySet()) {
        for (State newState : changedScHosts.get(compName).keySet()) {
          for (ServiceComponentHost scHost :
              changedScHosts.get(compName).get(newState)) {

            // Do not create role command for hosts that are not responding
            if (scHost.getHostState().equals(HostState.HEARTBEAT_LOST)) {
              LOG.info("Command is not created for servicecomponenthost "
                  + ", clusterName=" + cluster.getClusterName()
                  + ", clusterId=" + cluster.getClusterId()
                  + ", serviceName=" + scHost.getServiceName()
                  + ", componentName=" + scHost.getServiceComponentName()
                  + ", hostname=" + scHost.getHostName()
                  + ", hostState=" + scHost.getHostState()
                  + ", targetNewState=" + newState);
              continue;
            }

            RoleCommand roleCommand;
            State oldSchState = scHost.getState();
            ServiceComponentHostEvent event;
            switch (newState) {
              case INSTALLED:
                if (oldSchState == State.INIT
                    || oldSchState == State.UNINSTALLED
                    || oldSchState == State.INSTALLED
                    || oldSchState == State.INSTALLING
                    || oldSchState == State.UNKNOWN
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


                if (oldSchState == State.INSTALLED ||
                    oldSchState == State.STARTING ||
                    requestStages.getProjectedState(scHost.getHostName(),
                        scHost.getServiceComponentName()) == State.INSTALLED) {
                  roleCommand = RoleCommand.START;
                  event = new ServiceComponentHostStartEvent(
                      scHost.getServiceComponentName(), scHost.getHostName(),
                      nowTimestamp);
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
                  + ", requestId=" + requestStages.getId()
                  + ", componentName=" + scHost.getServiceComponentName()
                  + ", hostname=" + scHost.getHostName()
                  + ", roleCommand=" + roleCommand.name());
            }

            // [ type -> [ key, value ] ]
            Map<String, Map<String, String>> configurations = new TreeMap<String, Map<String, String>>();
            Host host = clusters.getHost(scHost.getHostName());

            Map<String, Map<String, String>> configTags =
              findConfigurationTagsWithOverrides(cluster, host.getHostName());

            // HACK - Set configs on the ExecCmd
            if (!scHost.getHostName().equals(jobtrackerHost)) {
              if (configTags.get(Configuration.GLOBAL_CONFIG_TAG) != null) {
                configHelper.applyCustomConfig(
                    configurations, Configuration.GLOBAL_CONFIG_TAG,
                    Configuration.RCA_ENABLED_PROPERTY, "false", false);
              }
            }

            createHostAction(cluster, stage, scHost, configurations, configTags,
              roleCommand, requestParameters, event);
          }
        }
      }

      for (String serviceName : smokeTestServices) { // Creates smoke test commands
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

        customCommandExecutionHelper.addServiceCheckAction(stage, clientHost,
          smokeTestRole, nowTimestamp, serviceName,
          null, null, createDefaultHostParams(cluster));
      }

      RoleCommandOrder rco = getRoleCommandOrder(cluster);
      RoleGraph rg = new RoleGraph(rco);
      rg.build(stage);
      return rg.getStages();
    }

    return null;
  }

  private TreeMap<String, String> createDefaultHostParams(Cluster cluster) {
    StackId stackId = cluster.getDesiredStackVersion();
    TreeMap<String, String> hostLevelParams = new TreeMap<String, String>();
    hostLevelParams.put(JDK_LOCATION, getJdkResourceUrl());
    hostLevelParams.put(JAVA_HOME, getJavaHome());
    hostLevelParams.put(JDK_NAME, getJDKName());
    hostLevelParams.put(JCE_NAME, getJCEName());
    hostLevelParams.put(STACK_NAME, stackId.getStackName());
    hostLevelParams.put(STACK_VERSION, stackId.getStackVersion());
    hostLevelParams.put(DB_NAME, getServerDB());
    hostLevelParams.put(MYSQL_JDBC_URL, getMysqljdbcUrl());
    hostLevelParams.put(ORACLE_JDBC_URL, getOjdbcUrl());
    hostLevelParams.put(DB_DRIVER_FILENAME, configs.getMySQLJarName());
    hostLevelParams.putAll(getRcaParameters());
    
    return hostLevelParams;
  }

  @Transactional
  void updateServiceStates(
      Map<State, List<Service>> changedServices,
      Map<State, List<ServiceComponent>> changedComps,
      Map<String, Map<State, List<ServiceComponentHost>>> changedScHosts,
      Collection<ServiceComponentHost> ignoredScHosts
  ) {
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

    if (ignoredScHosts != null) {
      for (ServiceComponentHost scHost : ignoredScHosts) {
        scHost.setDesiredState(scHost.getState());
      }
    }
  }

  @Override
  public RequestStatusResponse createAndPersistStages(Cluster cluster, Map<String, String> requestProperties,
                                                      Map<String, String> requestParameters,
                                                      Map<State, List<Service>> changedServices,
                                                      Map<State, List<ServiceComponent>> changedComponents,
                                                      Map<String, Map<State, List<ServiceComponentHost>>> changedHosts,
                                                      Collection<ServiceComponentHost> ignoredHosts,
                                                      boolean runSmokeTest, boolean reconfigureClients) throws AmbariException {

    RequestStageContainer request = addStages(null, cluster, requestProperties, requestParameters, changedServices,
        changedComponents, changedHosts, ignoredHosts, runSmokeTest, reconfigureClients);

    request.persist();
    return request.getRequestStatusResponse();
  }

  @Override
  public RequestStageContainer addStages(RequestStageContainer requestStages, Cluster cluster, Map<String, String> requestProperties,
                                 Map<String, String> requestParameters, Map<State, List<Service>> changedServices,
                                 Map<State, List<ServiceComponent>> changedComponents,
                                 Map<String, Map<State, List<ServiceComponentHost>>> changedHosts,
                                 Collection<ServiceComponentHost> ignoredHosts, boolean runSmokeTest,
                                 boolean reconfigureClients) throws AmbariException {

    if (requestStages == null) {
      requestStages = new RequestStageContainer(actionManager.getNextRequestId(), null, requestFactory, actionManager);
    }

    List<Stage> stages = doStageCreation(requestStages, cluster, changedServices, changedComponents,
        changedHosts, requestParameters, requestProperties.get(REQUEST_CONTEXT_PROPERTY),
        runSmokeTest, reconfigureClients);

    requestStages.addStages(stages);
    updateServiceStates(changedServices, changedComponents, changedHosts, ignoredHosts);
    return requestStages;
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
    Collection<ServiceComponentHost> ignoredScHosts =
        new ArrayList<ServiceComponentHost>();

    Set<String> clusterNames = new HashSet<String>();
    Map<String, Map<String, Map<String, Set<String>>>> hostComponentNames =
        new HashMap<String, Map<String, Map<String, Set<String>>>>();
    Set<State> seenNewStates = new HashSet<State>();
    Map<ServiceComponentHost, State> directTransitionScHosts = new HashMap<ServiceComponentHost, State>();
    Set<String> maintenanceClusters = new HashSet<String>();
    
    for (ServiceComponentHostRequest request : requests) {
      validateServiceComponentHostRequest(request);

      Cluster cluster = clusters.getCluster(request.getClusterName());

      if (StringUtils.isEmpty(request.getServiceName())) {
        request.setServiceName(findServiceName(cluster, request.getComponentName()));
      }

      LOG.info("Received a updateHostComponent request"
          + ", clusterName=" + request.getClusterName()
          + ", serviceName=" + request.getServiceName()
          + ", componentName=" + request.getComponentName()
          + ", hostname=" + request.getHostname()
          + ", request=" + request);

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
      
      if (null != request.getMaintenanceState()) {
        MaintenanceStateHelper psh = injector.getInstance(MaintenanceStateHelper.class);
        
        MaintenanceState newMaint = MaintenanceState.valueOf(request.getMaintenanceState());
        MaintenanceState oldMaint = psh.getEffectiveState(sch);
        
        if (newMaint != oldMaint) {
          if (sc.isClientComponent()) {
            throw new IllegalArgumentException("Invalid arguments, cannot set " +
              "maintenance state on a client component");
          } else if (newMaint.equals(MaintenanceState.IMPLIED)) {
            throw new IllegalArgumentException("Invalid arguments, can only set " +
              "maintenance state to one of " + EnumSet.of(MaintenanceState.OFF, MaintenanceState.ON));
          } else {
            sch.setMaintenanceState(newMaint);
            
            maintenanceClusters.add(sch.getClusterName());
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
              + ", oldState=" + oldState
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
      // Client component reinstall allowed
      if (newState == oldSchState && !sc.isClientComponent()) {
        ignoredScHosts.add(sch);
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

      if (!State.isValidStateTransition(oldSchState, newState)) {
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

    // Perform direct transitions (without task generation)
    for (Entry<ServiceComponentHost, State> entry : directTransitionScHosts.entrySet()) {
      ServiceComponentHost componentHost = entry.getKey();
      State newState = entry.getValue();
      long timestamp = System.currentTimeMillis();
      ServiceComponentHostEvent event;
      componentHost.setDesiredState(newState);
      switch (newState) {
        case DISABLED:
          event = new ServiceComponentHostDisableEvent(
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
    
    if (maintenanceClusters.size() > 0) {
      try {
        MaintenanceStateHelper.createRequests(this, requestProperties,
            maintenanceClusters);
      } catch (Exception e) {
        LOG.warn("Could not send maintenance status to Nagios (" + e.getMessage() + ")");
      }
    }

    Cluster cluster = clusters.getCluster(clusterNames.iterator().next());

    return createAndPersistStages(cluster, requestProperties, null, null, null, changedScHosts, ignoredScHosts, runSmokeTest,
        false);
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

    if (request.getAdminState() != null) {
      throw new IllegalArgumentException("Property adminState cannot be modified through update. Use service " +
          "specific DECOMMISSION action to decommision/recommission components.");
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
        if (oldState == State.DISABLED) {
          return true;
        }
        break;
      case DISABLED:
        if (oldState == State.INSTALLED ||
          oldState == State.UNKNOWN) {
          return true;
        }
        break;
      default:
        break;
    }
    return false;
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

      Set<String> roolesToDelete = new HashSet<String>(u.getRoles());
      Set<String> roolesToAdd = request.getRoles();
      roolesToDelete.removeAll(request.getRoles());
      for (String role : roolesToDelete) {
        users.removeRoleFromUser(u, role);
        u.getRoles().remove(role);
      }      
      roolesToAdd.removeAll(u.getRoles());
      for (String role : roolesToAdd) {
        users.addRoleToUser(u, role);
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
  public RequestStatusResponse deleteHostComponents(
      Set<ServiceComponentHostRequest> requests) throws AmbariException {

    Set<ServiceComponentHostRequest> expanded = new HashSet<ServiceComponentHostRequest>();
    
    // if any request are for the whole host, they need to be expanded
    for (ServiceComponentHostRequest request : requests) {
      if (null == request.getComponentName()) {
        if (null == request.getClusterName() || request.getClusterName().isEmpty() ||
            null == request.getHostname() || request.getHostname().isEmpty()) {
          throw new IllegalArgumentException("Cluster name and hostname must be specified.");
        }
        Cluster cluster = clusters.getCluster(request.getClusterName());
        
        for (ServiceComponentHost sch : cluster.getServiceComponentHosts(request.getHostname())) {
          ServiceComponentHostRequest schr = new ServiceComponentHostRequest(request.getClusterName(),
              sch.getServiceName(), sch.getServiceComponentName(), sch.getHostName(), null);
          expanded.add(schr);
        }
      }
      else {
        expanded.add(request);
      }
    }
    
    Map<ServiceComponent, Set<ServiceComponentHost>> safeToRemoveSCHs = new HashMap<ServiceComponent, Set<ServiceComponentHost>>();
    
    for (ServiceComponentHostRequest request : expanded) {

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

      // Only allow removing master/slave components in DISABLED/UNKNOWN/INSTALL_FAILED/INIT state without stages
      // generation.
      // Clients may be removed without a state check.
      if (!component.isClientComponent() &&
          !componentHost.getState().isRemovableState()) {
        throw new AmbariException("To remove master or slave components they must be in " +
            "DISABLED/INIT/INSTALLED/INSTALL_FAILED/UNKNOWN state. Current=" + componentHost.getState() + ".");
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

  /**
   * Get a request response for the given request ids.  Note that this method
   * fully populates a request resource including the set of task sub-resources
   * in the request response.
   */
  RequestStatusResponse getRequestStatusResponse(long requestId) {
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
  public Set<TaskStatusResponse> getTaskStatus(Set<TaskStatusRequest> requests)
      throws AmbariException {

    Collection<Long> requestIds = new ArrayList<Long>();
    Collection<Long> taskIds = new ArrayList<Long>();

    for (TaskStatusRequest request : requests) {
      if (request.getTaskId() != null) {
        taskIds.add(request.getTaskId());
      }
      if (request.getRequestId() != null) {
        requestIds.add(request.getRequestId());
      }
    }

    Set<TaskStatusResponse> responses = new HashSet<TaskStatusResponse>();
    for (HostRoleCommand command : actionManager.getTasksByRequestAndTaskIds(requestIds, taskIds)) {
      TaskStatusResponse taskStatusResponse = new TaskStatusResponse(command);
      responses.add(taskStatusResponse);
    }

    if (responses.size() == 0) {
      throw new ObjectNotFoundException("Task resource doesn't exist.");
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
  public Set<ServiceComponentHostResponse> getHostComponents(
      Set<ServiceComponentHostRequest> requests) throws AmbariException {
    LOG.debug("Processing requests: {}", requests);
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
        } else {
          LOG.debug("Ignoring not found exception due to other requests", e);
        }
      } catch (ServiceNotFoundException e) {
        if (requests.size() == 1) {
          // only throw exception if 1 request.
          // there will be > 1 request in case of OR predicate
          // In 'OR' case, a host_component may be included in predicate
          // that has no corresponding service
          throw e;
        } else {
          LOG.debug("Ignoring not found exception due to other requests", e);
        }
      } catch (ServiceComponentNotFoundException e) {
        if (requests.size() == 1) {
          // only throw exception if 1 request.
          // there will be > 1 request in case of OR predicate
          // In 'OR' case, a host_component may be included in predicate
          // that has no corresponding component
          throw e;
        } else {
          LOG.debug("Ignoring not found exception due to other requests", e);
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
              LOG.debug("HostNotFoundException ignored", e);
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
          return getHealthyHost(serviceComponent.getServiceComponentHosts().keySet());
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
      return getHealthyHost(serviceComponent.getServiceComponentHosts().keySet());
    }
    return null;
  }


  @Override
  public String getHealthyHost(Set<String> hostList) throws AmbariException {
    String hostName = null;
    for (String candidateHostName : hostList) {
      hostName = candidateHostName;
      Host candidateHost = clusters.getHost(hostName);
      if (candidateHost.getState() == HostState.HEALTHY) {
        break;
      }
    }
    return hostName;
  }

  @Override
  public RequestStatusResponse createAction(ExecuteActionRequest actionRequest, Map<String, String> requestProperties)
      throws AmbariException {
    String clusterName;
    String requestContext = "";

    if (requestProperties != null) {
      requestContext = requestProperties.get(REQUEST_CONTEXT_PROPERTY);
      if (requestContext == null) {
        // guice needs a non-null value as there is no way to mark this parameter @Nullable
        requestContext = "";
      }
    }

    if (actionRequest.getClusterName() == null
        || actionRequest.getClusterName().isEmpty()) {
      throw new AmbariException("Invalid request, cluster name must be specified");
    }
    clusterName = actionRequest.getClusterName();

    Cluster cluster = clusters.getCluster(clusterName);

    LOG.info("Received action execution request"
      + ", clusterName=" + actionRequest.getClusterName()
      + ", request=" + actionRequest.toString());

    ActionExecutionContext actionExecContext = getActionExecutionContext(actionRequest);
    if (actionRequest.isCommand()) {
      customCommandExecutionHelper.validateAction(actionRequest);
    } else {
      actionExecutionHelper.validateAction(actionRequest);
    }

    Map<String, Set<String>> clusterHostInfo = StageUtils.getClusterHostInfo(
        clusters.getHostsForCluster(cluster.getClusterName()), cluster);

    String clusterHostInfoJson = StageUtils.getGson().toJson(clusterHostInfo);
    Stage stage = createNewStage(0, cluster, actionManager.getNextRequestId(), requestContext, clusterHostInfoJson);

    Map<String, String> params = createDefaultHostParams(cluster);

    if (actionRequest.isCommand()) {
      customCommandExecutionHelper.addExecutionCommandsToStage(actionExecContext, stage, params);
    } else {
      actionExecutionHelper.addExecutionCommandsToStage(actionExecContext, stage, params);
    }

    RoleCommandOrder rco = this.getRoleCommandOrder(cluster);
    RoleGraph rg = new RoleGraph(rco);
    rg.build(stage);
    List<Stage> stages = rg.getStages();
    if (stages != null && !stages.isEmpty()) {
      actionManager.sendActions(stages, actionRequest);
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
  public synchronized RequestStatusResponse updateStacks() throws AmbariException {

    try {
      ambariMetaInfo.init();
    } catch (Exception e) {
      throw new AmbariException(
        "Ambari metainormation can't be read from the stack root directory");
    }

    return null;
  }

  @Override
  public Set<RepositoryResponse> getRepositories(Set<RepositoryRequest> requests)
      throws AmbariException {
    Set<RepositoryResponse> response = new HashSet<RepositoryResponse>();
    for (RepositoryRequest request : requests) {
      try {
        String stackName    = request.getStackName();
        String stackVersion = request.getStackVersion();

        Set<RepositoryResponse> repositories = getRepositories(request);

        for (RepositoryResponse repositoryResponse : repositories) {
          repositoryResponse.setStackName(stackName);
          repositoryResponse.setStackVersion(stackVersion);
        }
        response.addAll(repositories);
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
  public void updateRespositories(Set<RepositoryRequest> requests) throws AmbariException {
    for (RepositoryRequest rr : requests) {
      if (null == rr.getStackName() || rr.getStackName().isEmpty())
        throw new AmbariException("Stack name must be specified.");
      
      if (null == rr.getStackVersion() || rr.getStackVersion().isEmpty())
        throw new AmbariException("Stack version must be specified.");
      
      if (null == rr.getOsType() || rr.getOsType().isEmpty())
        throw new AmbariException("OS type must be specified.");
      
      if (null == rr.getRepoId() || rr.getRepoId().isEmpty())
        throw new AmbariException("Repo ID must be specified.");
      
      if (null != rr.getBaseUrl()) {
        if (!rr.isVerifyBaseUrl()) {
          ambariMetaInfo.updateRepoBaseURL(rr.getStackName(),
              rr.getStackVersion(), rr.getOsType(), rr.getRepoId(),
              rr.getBaseUrl());
        } else {
          URLStreamProvider usp = new URLStreamProvider(REPO_URL_CONNECT_TIMEOUT,
              REPO_URL_READ_TIMEOUT, null, null, null);

          boolean bFound = false;
          
          String[] suffixes = configs.getRepoValidationSuffixes();
          for (int i = 0; i < suffixes.length && !bFound; i++) {
            String suffix = suffixes[i];
            String spec = rr.getBaseUrl();
            
            if (spec.charAt(spec.length()-1) != '/' && suffix.charAt(0) != '/')
              spec = rr.getBaseUrl() + "/" + suffix;
            else if (spec.charAt(spec.length()-1) == '/' && suffix.charAt(0) == '/')
              spec = rr.getBaseUrl() + suffix.substring(1);
            else
              spec = rr.getBaseUrl() + suffix;
            
            try {
              IOUtils.readLines(usp.readFrom(spec));
              bFound = true;
            } catch (IOException ioe) {
              // failed, but try other suffixes
            }
          }
            
          if (bFound) {
            ambariMetaInfo.updateRepoBaseURL(rr.getStackName(),
                rr.getStackVersion(), rr.getOsType(), rr.getRepoId(),
                rr.getBaseUrl());
          } else {
            throw new IllegalArgumentException("Could not access base url '" +
                rr.getBaseUrl() + "'");
          }
        }
      }
    }
  }

  @Override
  public Set<StackVersionResponse> getStackVersions(
      Set<StackVersionRequest> requests) throws AmbariException {
    Set<StackVersionResponse> response = new HashSet<StackVersionResponse>();
    for (StackVersionRequest request : requests) {
      String stackName = request.getStackName();
      try {
        Set<StackVersionResponse> stackVersions = getStackVersions(request);
        for (StackVersionResponse stackVersionResponse : stackVersions) {
          stackVersionResponse.setStackName(stackName);
        }
        response.addAll(stackVersions);
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
    Set<StackVersionResponse> response;

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
      String stackName    = request.getStackName();
      String stackVersion = request.getStackVersion();

      try {
        Set<StackServiceResponse> stackServices = getStackServices(request);

        for (StackServiceResponse stackServiceResponse : stackServices) {
          stackServiceResponse.setStackName(stackName);
          stackServiceResponse.setStackVersion(stackVersion);
        }

        response.addAll(stackServices);
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
    Set<StackServiceResponse> response;

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

      String stackName    = request.getStackName();
      String stackVersion = request.getStackVersion();
      String serviceName  = request.getServiceName();

      Set<StackConfigurationResponse> stackConfigurations = getStackConfigurations(request);

      for (StackConfigurationResponse stackConfigurationResponse : stackConfigurations) {
        stackConfigurationResponse.setStackName(stackName);
        stackConfigurationResponse.setStackVersion(stackVersion);
        stackConfigurationResponse.setServiceName(serviceName);
      }

      response.addAll(stackConfigurations);
    }

    return response;
  }

  private Set<StackConfigurationResponse> getStackConfigurations(
      StackConfigurationRequest request) throws AmbariException {

    Set<StackConfigurationResponse> response;

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
      String stackName    = request.getStackName();
      String stackVersion = request.getStackVersion();
      String serviceName  = request.getServiceName();

      try {
        Set<StackServiceComponentResponse> stackComponents = getStackComponents(request);

        for (StackServiceComponentResponse stackServiceComponentResponse : stackComponents) {
          stackServiceComponentResponse.setStackName(stackName);
          stackServiceComponentResponse.setStackVersion(stackVersion);
          stackServiceComponentResponse.setServiceName(serviceName);
        }

        response.addAll(stackComponents);
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
    Set<StackServiceComponentResponse> response;

    String stackName     = request.getStackName();
    String stackVersion  = request.getStackVersion();
    String serviceName   = request.getServiceName();
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
        String stackName    = request.getStackName();
        String stackVersion = request.getStackVersion();

        Set<OperatingSystemResponse> stackOperatingSystems = getStackOperatingSystems(request);

        for (OperatingSystemResponse operatingSystemResponse : stackOperatingSystems) {
          operatingSystemResponse.setStackName(stackName);
          operatingSystemResponse.setStackVersion(stackVersion);
        }
        response.addAll(stackOperatingSystems);
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

    Set<OperatingSystemResponse> response;

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
  
  @Override
  public String getAuthName() {
    return AuthorizationHelper.getAuthenticatedName(configs.getAnonymousAuditName());
  }

  @Override
  public Set<RootServiceResponse> getRootServices(
      Set<RootServiceRequest> requests) throws AmbariException {
    Set<RootServiceResponse> response = new HashSet<RootServiceResponse>();
    for (RootServiceRequest request : requests) {
      try {
        response.addAll(getRootServices(request));
      } catch (AmbariException e) {
        if (requests.size() == 1) {
          // only throw exception if 1 request.
          // there will be > 1 request in case of OR predicate
          throw e;
        }
      }
    }
    return response;
  }

  private Set<RootServiceResponse> getRootServices (RootServiceRequest request)
      throws AmbariException{
    return this.rootServiceResponseFactory.getRootServices(request);
  }

  @Override
  public Set<RootServiceComponentResponse> getRootServiceComponents(
      Set<RootServiceComponentRequest> requests) throws AmbariException {
    Set<RootServiceComponentResponse> response = new HashSet<RootServiceComponentResponse>();
    for (RootServiceComponentRequest request : requests) {
      String serviceName  = request.getServiceName();
      try {
        Set<RootServiceComponentResponse> rootServiceComponents = getRootServiceComponents(request);

        for (RootServiceComponentResponse serviceComponentResponse : rootServiceComponents) {
          serviceComponentResponse.setServiceName(serviceName);
        }

        response.addAll(rootServiceComponents);
      } catch (AmbariException e) {
        if (requests.size() == 1) {
          // only throw exception if 1 request.
          // there will be > 1 request in case of OR predicate
          throw e;
        }
      }
    }
    return response;
  }

  private Set<RootServiceComponentResponse> getRootServiceComponents(
      RootServiceComponentRequest request) throws AmbariException{
    return this.rootServiceResponseFactory.getRootServiceComponents(request);
  }

  @Override
  public Clusters getClusters() {
    return clusters;
  }

  @Override
  public AmbariMetaInfo getAmbariMetaInfo() {
    return ambariMetaInfo;
  }

  @Override
  public ServiceFactory getServiceFactory() {
    return serviceFactory;
  }

  @Override
  public ServiceComponentFactory getServiceComponentFactory() {
    return serviceComponentFactory;
  }

  @Override
  public ConfigGroupFactory getConfigGroupFactory() {
    return configGroupFactory;
  }

  @Override
  public AbstractRootServiceResponseFactory getRootServiceResponseFactory() {
    return rootServiceResponseFactory;

  }

  @Override
  public ActionManager getActionManager() {
    return actionManager;
  }

  @Override
  public String getJdkResourceUrl() {
    return jdkResourceUrl;
  }

  @Override
  public String getJavaHome() {
    return javaHome;
  }

  @Override
  public String getJDKName() {
    return jdkName;
  }

  @Override
  public String getJCEName() {
    return jceName;
  }

  @Override
  public String getServerDB() {
    return serverDB;
  }

  @Override
  public String getOjdbcUrl() {
    return ojdbcUrl;
  }

  @Override
  public String getMysqljdbcUrl() {
    return mysqljdbcUrl;
  }
  
  public Map<String, String> getRcaParameters() {

    String hostName = StageUtils.getHostName();

    String url = configs.getRcaDatabaseUrl();
    if (url.contains(Configuration.HOSTNAME_MACRO))
      url =
          url.replace(Configuration.HOSTNAME_MACRO,
              hostsMap.getHostMap(hostName));

    Map<String, String> rcaParameters = new HashMap<String, String>();

    rcaParameters.put(AMBARI_DB_RCA_URL, url);
    rcaParameters.put(AMBARI_DB_RCA_DRIVER, configs.getRcaDatabaseDriver());
    rcaParameters.put(AMBARI_DB_RCA_USERNAME, configs.getRcaDatabaseUser());
    rcaParameters.put(AMBARI_DB_RCA_PASSWORD, configs.getRcaDatabasePassword());

    return rcaParameters;
  }
  
}
