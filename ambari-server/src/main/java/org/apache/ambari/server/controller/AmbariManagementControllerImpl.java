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

package org.apache.ambari.server.controller;

import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.AMBARI_DB_RCA_DRIVER;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.AMBARI_DB_RCA_PASSWORD;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.AMBARI_DB_RCA_URL;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.AMBARI_DB_RCA_USERNAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.CLIENTS_TO_UPDATE_CONFIGS;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.COMMAND_RETRY_ENABLED;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.COMMAND_TIMEOUT;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.CUSTOM_FOLDER;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.DB_DRIVER_FILENAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.GROUP_LIST;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.HOOKS_FOLDER;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.MAX_DURATION_OF_RETRIES;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.NOT_MANAGED_HDFS_PATH_LIST;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.PACKAGE_LIST;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.PACKAGE_VERSION;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.REPO_INFO;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SCRIPT;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SCRIPT_TYPE;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SERVICE_PACKAGE_FOLDER;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SERVICE_REPO_INFO;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.USER_LIST;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.VERSION;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import javax.persistence.RollbackException;

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
import org.apache.ambari.server.actionmanager.CommandExecutionType;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.agent.ExecutionCommand.KeyNames;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.LoggingService;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.configuration.Configuration.DatabaseType;
import org.apache.ambari.server.controller.internal.DeleteStatusMetaData;
import org.apache.ambari.server.controller.internal.RequestOperationLevel;
import org.apache.ambari.server.controller.internal.RequestResourceFilter;
import org.apache.ambari.server.controller.internal.RequestStageContainer;
import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.apache.ambari.server.controller.internal.WidgetLayoutResourceProvider;
import org.apache.ambari.server.controller.internal.WidgetResourceProvider;
import org.apache.ambari.server.controller.logging.LoggingSearchPropertyProvider;
import org.apache.ambari.server.controller.metrics.MetricPropertyProviderFactory;
import org.apache.ambari.server.controller.metrics.MetricsCollectorHAManager;
import org.apache.ambari.server.controller.metrics.timeline.cache.TimelineMetricCacheProvider;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.customactions.ActionDefinition;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.metadata.RoleCommandOrder;
import org.apache.ambari.server.metadata.RoleCommandOrderProvider;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterVersionDAO;
import org.apache.ambari.server.orm.dao.ExtensionDAO;
import org.apache.ambari.server.orm.dao.ExtensionLinkDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.dao.WidgetDAO;
import org.apache.ambari.server.orm.dao.WidgetLayoutDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.ExtensionEntity;
import org.apache.ambari.server.orm.entities.ExtensionLinkEntity;
import org.apache.ambari.server.orm.entities.OperatingSystemEntity;
import org.apache.ambari.server.orm.entities.RepositoryEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.orm.entities.WidgetEntity;
import org.apache.ambari.server.orm.entities.WidgetLayoutEntity;
import org.apache.ambari.server.orm.entities.WidgetLayoutUserWidgetEntity;
import org.apache.ambari.server.scheduler.ExecutionScheduleManager;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.Group;
import org.apache.ambari.server.security.authorization.GroupType;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.security.authorization.User;
import org.apache.ambari.server.security.authorization.UserType;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.security.credential.PrincipalKeyCredential;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.security.encryption.CredentialStoreType;
import org.apache.ambari.server.security.ldap.AmbariLdapDataPopulator;
import org.apache.ambari.server.security.ldap.LdapBatchDto;
import org.apache.ambari.server.security.ldap.LdapSyncDto;
import org.apache.ambari.server.serveraction.kerberos.KerberosInvalidConfigurationException;
import org.apache.ambari.server.serveraction.kerberos.KerberosOperationException;
import org.apache.ambari.server.stack.ExtensionHelper;
import org.apache.ambari.server.stack.RepoUtil;
import org.apache.ambari.server.stageplanner.RoleGraph;
import org.apache.ambari.server.stageplanner.RoleGraphFactory;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.CommandScriptDefinition;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.ExtensionInfo;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostComponentAdminState;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.OperatingSystemInfo;
import org.apache.ambari.server.state.PropertyDependencyInfo;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.PropertyInfo.PropertyType;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.SecurityType;
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
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.server.state.scheduler.RequestExecutionFactory;
import org.apache.ambari.server.state.stack.RepositoryXml;
import org.apache.ambari.server.state.stack.WidgetLayout;
import org.apache.ambari.server.state.stack.WidgetLayoutInfo;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostInstallEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpInProgressEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStartEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStopEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostUpgradeEvent;
import org.apache.ambari.server.topology.Setting;
import org.apache.ambari.server.utils.SecretReference;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class AmbariManagementControllerImpl implements AmbariManagementController {

  private final static Logger LOG =
      LoggerFactory.getLogger(AmbariManagementControllerImpl.class);

  /**
   * Property name of request context.
   */
  private static final String REQUEST_CONTEXT_PROPERTY = "context";

  private static final String CLUSTER_PHASE_PROPERTY = "phase";
  private static final String CLUSTER_PHASE_INITIAL_INSTALL = "INITIAL_INSTALL";
  private static final String CLUSTER_PHASE_INITIAL_START = "INITIAL_START";

  private static final String BASE_LOG_DIR = "/tmp/ambari";

  private static final String PASSWORD = "password";
  public static final String SKIP_INSTALL_FOR_COMPONENTS = "skipInstallForComponents";
  public static final String DONT_SKIP_INSTALL_FOR_COMPONENTS = "dontSkipInstallForComponents";

  private final Clusters clusters;

  private final ActionManager actionManager;

  private final Injector injector;

  private final Gson gson;


  @Inject
  private RoleCommandOrderProvider roleCommandOrderProvider;

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
  private RoleGraphFactory roleGraphFactory;
  @Inject
  private ConfigGroupFactory configGroupFactory;
  @Inject
  private ConfigHelper configHelper;
  @Inject
  private RequestExecutionFactory requestExecutionFactory;
  @Inject
  private ExecutionScheduleManager executionScheduleManager;
  @Inject
  private AmbariLdapDataPopulator ldapDataPopulator;
  @Inject
  private RepositoryVersionDAO repositoryVersionDAO;
  @Inject
  private WidgetDAO widgetDAO;
  @Inject
  private WidgetLayoutDAO widgetLayoutDAO;
  @Inject
  private ClusterDAO clusterDAO;
  @Inject
  private CredentialStoreService credentialStoreService;
  @Inject
  private ClusterVersionDAO clusterVersionDAO;
  @Inject
  private AmbariEventPublisher ambariEventPublisher;
  @Inject
  private MetricsCollectorHAManager metricsCollectorHAManager;

  private MaintenanceStateHelper maintenanceStateHelper;

  @Inject
  private ExtensionLinkDAO linkDAO;
  @Inject
  private ExtensionDAO extensionDAO;
  @Inject
  private StackDAO stackDAO;

  /**
   * The KerberosHelper to help setup for enabling for disabling Kerberos
   */
  private KerberosHelper kerberosHelper;

  final private String masterHostname;
  final private Integer masterPort;
  final private String masterProtocol;

  final private static String JDK_RESOURCE_LOCATION =
      "/resources/";

  final private static int REPO_URL_CONNECT_TIMEOUT = 3000;
  final private static int REPO_URL_READ_TIMEOUT = 2000;

  final private String jdkResourceUrl;
  final private String javaHome;
  final private String jdkName;
  final private String jceName;
  final private String ojdbcUrl;
  final private String serverDB;
  final private String mysqljdbcUrl;

  private boolean ldapSyncInProgress;

  private Cache<ClusterRequest, ClusterResponse> clusterUpdateCache =
      CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build();

  @Inject
  private AmbariCustomCommandExecutionHelper customCommandExecutionHelper;
  @Inject
  private AmbariActionExecutionHelper actionExecutionHelper;

  private Map<String, Map<String, Map<String, String>>> configCredentialsForService = new HashMap<>();

  @Inject
  public AmbariManagementControllerImpl(ActionManager actionManager,
      Clusters clusters, Injector injector) throws Exception {
    this.clusters = clusters;
    this.actionManager = actionManager;
    this.injector = injector;
    injector.injectMembers(this);
    gson = injector.getInstance(Gson.class);
    LOG.info("Initializing the AmbariManagementControllerImpl");
    masterHostname =  InetAddress.getLocalHost().getCanonicalHostName();
    maintenanceStateHelper = injector.getInstance(MaintenanceStateHelper.class);
    kerberosHelper = injector.getInstance(KerberosHelper.class);
    if(configs != null)
    {
      if (configs.getApiSSLAuthentication()) {
        masterProtocol = "https";
        masterPort = configs.getClientSSLApiPort();
      } else {
        masterProtocol = "http";
        masterPort = configs.getClientApiPort();
      }
      jdkResourceUrl = getAmbariServerURI(JDK_RESOURCE_LOCATION);
      javaHome = configs.getJavaHome();
      jdkName = configs.getJDKName();
      jceName = configs.getJCEName();
      ojdbcUrl = getAmbariServerURI(JDK_RESOURCE_LOCATION + "/" + configs.getOjdbcJarName());
      mysqljdbcUrl = getAmbariServerURI(JDK_RESOURCE_LOCATION + "/" + configs.getMySQLJarName());

      serverDB = configs.getServerDBName();
    } else {
      masterProtocol = null;
      masterPort = null;

      jdkResourceUrl = null;
      javaHome = null;
      jdkName = null;
      jceName = null;
      ojdbcUrl = null;
      mysqljdbcUrl = null;
      serverDB = null;
    }
  }

  @Override
  public String getAmbariServerURI(String path) {
    if(masterProtocol==null || masterHostname==null || masterPort==null) {
      return null;
    }

    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setScheme(masterProtocol);
    uriBuilder.setHost(masterHostname);
    uriBuilder.setPort(masterPort);

    String[] parts = path.split("\\?");

    if (parts.length > 1) {
      uriBuilder.setPath(parts[0]);
      uriBuilder.setQuery(parts[1]);
    } else {
      uriBuilder.setPath(path);
    }

    return uriBuilder.toString();
  }

  @Override
  public RoleCommandOrder getRoleCommandOrder(Cluster cluster) {
      return roleCommandOrderProvider.getRoleCommandOrder(cluster);
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
    StackInfo stackInfo = ambariMetaInfo.getStack(stackId.getStackName(),
      stackId.getStackVersion());

    if (stackInfo == null) {
      throw new StackAccessException("stackName=" + stackId.getStackName() + ", stackVersion=" + stackId.getStackVersion());
    }

    RepositoryVersionEntity versionEntity = null;

    if (null != request.getRepositoryVersion()) {
      versionEntity = repositoryVersionDAO.findByStackAndVersion(stackId,
          request.getRepositoryVersion());

      if (null == versionEntity) {
        throw new AmbariException(String.format("Tried to create a cluster on version %s, but that version doesn't exist",
            request.getRepositoryVersion()));
      }
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

    clusters.addCluster(request.getClusterName(), stackId, request.getSecurityType());
    Cluster c = clusters.getCluster(request.getClusterName());

    if (request.getHostNames() != null) {
      clusters.mapAndPublishHostsToCluster(request.getHostNames(),
          request.getClusterName());
    }
    // Create cluster widgets and layouts
    initializeWidgetsAndLayouts(c, null);

    if (null != versionEntity) {
      ClusterVersionDAO clusterVersionDAO = injector.getInstance(ClusterVersionDAO.class);

      ClusterVersionEntity clusterVersion = clusterVersionDAO.findByClusterAndStackAndVersion(request.getClusterName(), stackId,
          request.getRepositoryVersion());

      if (null == clusterVersion) {
        c.createClusterVersion(stackId, versionEntity.getVersion(), getAuthName(), RepositoryVersionState.INIT);
      }
    }

  }

  @Override
  public synchronized void createHostComponents(Set<ServiceComponentHostRequest> requests)
      throws AmbariException, AuthorizationException {

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

      if(!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, cluster.getResourceId(),
          EnumSet.of(RoleAuthorization.SERVICE_ADD_DELETE_SERVICES,RoleAuthorization.HOST_ADD_DELETE_COMPONENTS))) {
        throw new AuthorizationException("The authenticated user is not authorized to install service components on to hosts");
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
          .get(request.getServiceName())
          .get(request.getComponentName())
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

      setRestartRequiredServices(s, request.getComponentName());

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

    // set restartRequired flag for  monitoring services
    setMonitoringServicesRestartRequired(requests);
    // now doing actual work
    persistServiceComponentHosts(requests);
  }

  void persistServiceComponentHosts(Set<ServiceComponentHostRequest> requests)
    throws AmbariException {
    Multimap<Cluster, ServiceComponentHost> schMap = ArrayListMultimap.create();

    for (ServiceComponentHostRequest request : requests) {
      Cluster cluster = clusters.getCluster(request.getClusterName());
      Service s = cluster.getService(request.getServiceName());
      ServiceComponent sc = s.getServiceComponent(
          request.getComponentName());

      ServiceComponentHost sch =
          serviceComponentHostFactory.createNew(sc, request.getHostname());

      if (request.getDesiredState() != null
          && !request.getDesiredState().isEmpty()) {
        State state = State.valueOf(request.getDesiredState());
        sch.setDesiredState(state);
      }

      sch.setDesiredStackVersion(sc.getDesiredStackVersion());

      schMap.put(cluster, sch);
    }

    for (Cluster cluster : schMap.keySet()) {
      cluster.addServiceComponentHosts(schMap.get(cluster));
    }
  }

  private void setMonitoringServicesRestartRequired(
    Set<ServiceComponentHostRequest> requests) throws AmbariException {

    for (ServiceComponentHostRequest request : requests) {
      Cluster cluster = clusters.getCluster(request.getClusterName());

      StackId stackId = cluster.getCurrentStackVersion();
      Collection<String> monitoringServices = ambariMetaInfo.getMonitoringServiceNames(
        stackId.getStackName(), stackId.getStackVersion());

      for (String serviceName : monitoringServices) {
        if (cluster.getServices().containsKey(serviceName)) {
          Service service = cluster.getService(serviceName);

          for (ServiceComponent sc : service.getServiceComponents().values()) {
            if (sc.isMasterComponent()) {
              for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
                sch.setRestartRequired(true);
              }
              continue;
            }

            String hostname = request.getHostname();
            if (sc.getServiceComponentHosts().containsKey(hostname)) {
              ServiceComponentHost sch = sc.getServiceComponentHost(hostname);
              sch.setRestartRequired(true);
            }
          }
        }
      }
    }
  }

  private void setRestartRequiredServices(
          Service service, String componentName) throws AmbariException {
    Cluster cluster = service.getCluster();
    StackId stackId = cluster.getCurrentStackVersion();
    if (service.getServiceComponent(componentName).isClientComponent()) {
      return;
    }
    Set<String> needRestartServices = ambariMetaInfo.getRestartRequiredServicesNames(
      stackId.getStackName(), stackId.getStackVersion());

    if(needRestartServices.contains(service.getName())) {
      Map<String, ServiceComponent> m = service.getServiceComponents();
      for (Entry<String, ServiceComponent> entry : m.entrySet()) {
        ServiceComponent serviceComponent = entry.getValue();
        Map<String, ServiceComponentHost> schMap = serviceComponent.getServiceComponentHosts();

          for (Entry<String, ServiceComponentHost> sch : schMap.entrySet()) {
            ServiceComponentHost serviceComponentHost = sch.getValue();
            serviceComponentHost.setRestartRequired(true);
          }

      }
    }
  }

  @Override
  public void registerRackChange(String clusterName) throws AmbariException {
    Cluster cluster = clusters.getCluster(clusterName);
    StackId stackId = cluster.getCurrentStackVersion();

    Set<String> rackSensitiveServices =
        ambariMetaInfo.getRackSensitiveServicesNames(stackId.getStackName(), stackId.getStackVersion());

    Map<String, Service> services = cluster.getServices();

    for (Service service : services.values()) {
      if(rackSensitiveServices.contains(service.getName())) {
        Map<String, ServiceComponent> serviceComponents = service.getServiceComponents();
        for (ServiceComponent serviceComponent : serviceComponents.values()) {
          Map<String, ServiceComponentHost> schMap = serviceComponent.getServiceComponentHosts();
          for (Entry<String, ServiceComponentHost> sch : schMap.entrySet()) {
            ServiceComponentHost serviceComponentHost = sch.getValue();
            serviceComponentHost.setRestartRequired(true);
          }
        }
      }
    }
  }

  /**
   * Creates a configuration.
   * <p>
   * This implementation ensures the authenticated user is authorized to create the new configuration
   * based on the details of what properties are being changed and the authorizations the authenticated
   * user has been granted.
   * <p>
   * Example
   * <ul>
   * <li>
   * If the user is attempting to change a service-level configuration that user must be granted the
   * <code>SERVICE_MODIFY_CONFIGS</code> privilege (authorization)
   * </li>
   * <li>
   * If the user is attempting to change the cluster-wide value to enable or disable auto-start
   * (<code>cluster-env/recovery_enabled</code>), that user must be granted the
   * <code>CLUSTER_MANAGE_AUTO_START</code> privilege (authorization)
   * </li>
   * </ul>
   *
   * @param request the request object which defines the configuration.
   * @throws AmbariException when the configuration cannot be created.
   */
  @Override
  public synchronized ConfigurationResponse createConfiguration(
      ConfigurationRequest request) throws AmbariException, AuthorizationException {
    if (null == request.getClusterName() || request.getClusterName().isEmpty()
        || null == request.getType() || request.getType().isEmpty()
        || null == request.getProperties()) {
      throw new IllegalArgumentException("Invalid Arguments,"
          + " clustername, config type and configs should not"
          + " be null or empty");
    }

    Cluster cluster = clusters.getCluster(request.getClusterName());

    String configType = request.getType();

    // If the config type is for a service, then allow a user with SERVICE_MODIFY_CONFIGS to
    // update, else ensure the user has CLUSTER_MODIFY_CONFIGS
    String service = null;

    try {
      service = cluster.getServiceForConfigTypes(Collections.singleton(configType));
    } catch (IllegalArgumentException e) {
      // Ignore this since we may have hit a config type that spans multiple services. This may
      // happen in unit test cases but should not happen with later versions of stacks.
    }

    // Get the changes so that the user's intention can be determined. For example, maybe
    // the user wants to change the run-as user for a service or maybe the the cluster-wide
    // recovery mode setting.
    Map<String, String[]> propertyChanges = getPropertyChanges(cluster, request);

    if(StringUtils.isEmpty(service)) {
      // If the configuration is not attached to a specific service, it is a cluster-wide configuration
      // type. For example, cluster-env.

      // If the user is trying to set the cluster-wide recovery mode, ensure that user
      // has the appropriate authorization
      validateAuthorizationToManageServiceAutoStartConfiguration(cluster, configType, propertyChanges);

      // If the user is trying to set any other cluster-wide property, ensure that user
      // has the appropriate authorization
      validateAuthorizationToModifyConfigurations(cluster, configType, propertyChanges,
          Collections.singletonMap("cluster-env", Collections.singleton("recovery_enabled")),
          false);
    }
    else {
      // If the user is trying to set any service-level property, ensure that user
      // has the appropriate authorization
      validateAuthorizationToModifyConfigurations(cluster, configType, propertyChanges, null, true);

      // Ensure the user is allowed to update service users and groups.
      validateAuthorizationToUpdateServiceUsersAndGroups(cluster, configType, propertyChanges);
    }

    Map<String, String> requestProperties = request.getProperties();

    // Configuration attributes are optional. If not present, use default(provided by stack), otherwise merge default
    // with request-provided
    Map<String, Map<String, String>> requestPropertiesAttributes = request.getPropertiesAttributes();

    if (requestPropertiesAttributes != null && requestPropertiesAttributes.containsKey(PASSWORD)) {
      for (Map.Entry<String, String> requestEntry : requestPropertiesAttributes.get(PASSWORD).entrySet()) {
        String passwordProperty = requestEntry.getKey();
        if(requestProperties.containsKey(passwordProperty) && requestEntry.getValue().equals("true")) {
          String passwordPropertyValue = requestProperties.get(passwordProperty);
          if (!SecretReference.isSecret(passwordPropertyValue)) {
            continue;
          }
          SecretReference ref = new SecretReference(passwordPropertyValue, cluster);
          String refValue = ref.getValue();
          requestProperties.put(passwordProperty, refValue);
        }
      }
    }

    Map<PropertyInfo.PropertyType, Set<String>> propertiesTypes = cluster.getConfigPropertiesTypes(request.getType());
    if(propertiesTypes.containsKey(PropertyType.PASSWORD)) {
      for(String passwordProperty : propertiesTypes.get(PropertyType.PASSWORD)) {
        if(requestProperties.containsKey(passwordProperty)) {
          String passwordPropertyValue = requestProperties.get(passwordProperty);
          if (!SecretReference.isSecret(passwordPropertyValue)) {
            continue;
          }
          SecretReference ref = new SecretReference(passwordPropertyValue, cluster);
          String refValue = ref.getValue();
          requestProperties.put(passwordProperty, refValue);
        }
      }
    }



    Map<String, Config> configs = cluster.getConfigsByType(
        request.getType());
    if (null == configs) {
      configs = new HashMap<String, Config>();
    }

    Map<String, Map<String, String>> propertiesAttributes = new HashMap<String, Map<String,String>>();

    StackId currentStackId = cluster.getCurrentStackVersion();
    StackInfo currentStackInfo = ambariMetaInfo.getStack(currentStackId.getStackName(), currentStackId.getStackVersion());
    Map<String, Map<String, String>> defaultConfigAttributes = currentStackInfo.getDefaultConfigAttributesForConfigType(configType);

    if(defaultConfigAttributes != null){
      ConfigHelper.mergeConfigAttributes(propertiesAttributes, defaultConfigAttributes);
    }
    // overwrite default attributes with request attributes
    if(requestPropertiesAttributes != null){
      ConfigHelper.mergeConfigAttributes(propertiesAttributes, requestPropertiesAttributes);
    }

    if (configs.containsKey(request.getVersionTag())) {
      throw new AmbariException(MessageFormat.format("Configuration with tag ''{0}'' exists for ''{1}''",
          request.getVersionTag(),
          request.getType()));
    }

    Config config = createConfig(cluster, request.getType(), requestProperties,
      request.getVersionTag(), propertiesAttributes);

    LOG.info(MessageFormat.format("Creating configuration with tag ''{0}'' to cluster ''{1}''  for configuration type {2}",
        request.getVersionTag(),
        request.getClusterName(),
        configType));

    return new ConfigurationResponse(cluster.getClusterName(), config);
  }

  @Override
  public Config createConfig(Cluster cluster, String type, Map<String, String> properties,
                             String versionTag, Map<String, Map<String, String>> propertiesAttributes) {

    Config config = configFactory.createNew(cluster, type, versionTag, properties,
        propertiesAttributes);

    cluster.addConfig(config);
    return config;
  }

  @Override
  public void createUsers(Set<UserRequest> requests) throws AmbariException {

    for (UserRequest request : requests) {

      if (null == request.getUsername() || request.getUsername().isEmpty() ||
          null == request.getPassword() || request.getPassword().isEmpty()) {
        throw new AmbariException("Username and password must be supplied.");
      }

      users.createUser(request.getUsername(), request.getPassword(), UserType.LOCAL, request.isActive(), request.isAdmin());
    }
  }

  @Override
  public void createGroups(Set<GroupRequest> requests) throws AmbariException {
    for (GroupRequest request : requests) {
      if (StringUtils.isBlank(request.getGroupName())) {
        throw new AmbariException("Group name must be supplied.");
      }
      final Group group = users.getGroup(request.getGroupName());
      if (group != null) {
        throw new AmbariException("Group already exists.");
      }
      users.createGroup(request.getGroupName(), GroupType.LOCAL);
    }
  }

  @Override
  public void createMembers(Set<MemberRequest> requests) throws AmbariException {
    for (MemberRequest request : requests) {
      if (StringUtils.isBlank(request.getGroupName()) || StringUtils.isBlank(request.getUserName())) {
        throw new AmbariException("Both group name and user name must be supplied.");
      }
      users.addMemberToGroup(request.getGroupName(), request.getUserName());
    }
  }

  @Override
  public Set<MemberResponse> getMembers(Set<MemberRequest> requests)
      throws AmbariException {
    final Set<MemberResponse> responses = new HashSet<MemberResponse>();
    for (MemberRequest request: requests) {
      LOG.debug("Received a getMembers request, " + request.toString());
      final Group group = users.getGroup(request.getGroupName());
      if (null == group) {
        if (requests.size() == 1) {
          // only throw exception if there is a single request
          // if there are multiple requests, this indicates an OR predicate
          throw new ObjectNotFoundException("Cannot find group '"
              + request.getGroupName() + "'");
        }
      } else {
        for (User user: users.getGroupMembers(group.getGroupName())) {
          final MemberResponse response = new MemberResponse(group.getGroupName(), user.getUserName());
          responses.add(response);
        }
      }
    }
    return responses;
  }

  @Override
  @SuppressWarnings("unchecked")
  public synchronized void updateMembers(Set<MemberRequest> requests) throws AmbariException {
    // validate
    String groupName = null;
    for (MemberRequest request: requests) {
      if (groupName != null && !request.getGroupName().equals(groupName)) {
        throw new AmbariException("Can't manage members of different groups in one request");
      }
      groupName = request.getGroupName();
    }
    final List<String> requiredMembers = new ArrayList<String>();
    for (MemberRequest request: requests) {
      if (request.getUserName() != null) {
        requiredMembers.add(request.getUserName());
      }
    }
    final List<String> currentMembers = users.getAllMembers(groupName);
    for (String user: (Collection<String>) CollectionUtils.subtract(currentMembers, requiredMembers)) {
      users.removeMemberFromGroup(groupName, user);
    }
    for (String user: (Collection<String>) CollectionUtils.subtract(requiredMembers, currentMembers)) {
      users.addMemberToGroup(groupName, user);
    }
  }

  private Stage createNewStage(long id, Cluster cluster, long requestId,
                               String requestContext, String clusterHostInfo,
                               String commandParamsStage, String hostParamsStage) {
    String logDir = BASE_LOG_DIR + File.pathSeparator + requestId;
    Stage stage =
        stageFactory.createNew(requestId, logDir,
          null == cluster ? null : cluster.getClusterName(),
          null == cluster ? -1L : cluster.getClusterId(),
          requestContext, clusterHostInfo, commandParamsStage,
          hostParamsStage);
    stage.setStageId(id);
    return stage;
  }

  private Set<ClusterResponse> getClusters(ClusterRequest request)
      throws AmbariException, AuthorizationException {

    Set<ClusterResponse> response = new HashSet<ClusterResponse>();

    if (LOG.isDebugEnabled()) {
      LOG.debug("Received a getClusters request"
        + ", clusterName=" + request.getClusterName()
        + ", clusterId=" + request.getClusterId()
        + ", stackInfo=" + request.getStackVersion());
    }

    Cluster singleCluster = null;
    try {
      if (request.getClusterName() != null) {
        singleCluster = clusters.getCluster(request.getClusterName());
      } else if (request.getClusterId() != null) {
        singleCluster = clusters.getClusterById(request.getClusterId());
      }
    }
    catch(ClusterNotFoundException e) {
      // the user shouldn't know the difference between a cluster that does not exist or one that
      // he doesn't have access to.
      if (AuthorizationHelper.isAuthorized(ResourceType.AMBARI, null, RoleAuthorization.AMBARI_ADD_DELETE_CLUSTERS)) {
        throw e;
      } else {
        throw new AuthorizationException();
      }
    }

    if (singleCluster != null) {
      ClusterResponse cr = singleCluster.convertToResponse();
      cr.setDesiredConfigs(singleCluster.getDesiredConfigs());
      cr.setDesiredServiceConfigVersions(singleCluster.getActiveServiceConfigVersions());
      cr.setCredentialStoreServiceProperties(getCredentialStoreServiceProperties());

     // If the user is authorized to view information about this cluster, add it to the response
// TODO: Uncomment this when the UI doesn't require view access for View-only users.
//      if (AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, cr.getResourceId(),
//          RoleAuthorization.AUTHORIZATIONS_VIEW_CLUSTER)) {
      response.add(cr);
//      }
//      else {
//        // the user shouldn't know the difference between a cluster that does not exist or one that
//        // he doesn't have access to.
//        throw new AuthorizationException();
//      }

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

// TODO: Uncomment this when the UI doesn't require view access for View-only users.
//       If the user is authorized to view information about this cluster, add it to the response
//       if (AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, c.getResourceId(),
//        RoleAuthorization.AUTHORIZATIONS_VIEW_CLUSTER)) {
      ClusterResponse cr = c.convertToResponse();
      cr.setDesiredConfigs(c.getDesiredConfigs());
      cr.setDesiredServiceConfigVersions(c.getActiveServiceConfigVersions());
      cr.setCredentialStoreServiceProperties(getCredentialStoreServiceProperties());
      response.add(cr);
//       }
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
          LOG.error("Host doesn't belong to cluster - " + request.getHostname());
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
    boolean checkState = false;
    State stateToCheck = null;
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

    if (!StringUtils.isEmpty(request.getState())) {
      stateToCheck = State.valueOf(request.getState());
      // maybe check should be more wider
      if (stateToCheck == null) {
        throw new IllegalArgumentException("Invalid arguments, invalid state, State=" + request.getState());
      }
      checkState = true;
    }

    Map<String, DesiredConfig> desiredConfigs = cluster.getDesiredConfigs();
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
              throw new ServiceComponentHostNotFoundException(cluster.getClusterName(),
                s.getName(), sc.getName(), request.getHostname());
            }

            ServiceComponentHost sch = serviceComponentHostMap.get(request.getHostname());

            if (null == sch) {
              // It's possible that the host was deleted during the time that the request was generated.
              continue;
            }

            if (checkDesiredState && (desiredStateToCheck != sch.getDesiredState())) {
              continue;
            }

            if (checkState && stateToCheck != sch.getState()) {
              continue;
            }

            if (request.getAdminState() != null) {
              String stringToMatch =
                  sch.getComponentAdminState() == null ? "" : sch.getComponentAdminState().name();
              if (!request.getAdminState().equals(stringToMatch)) {
                continue;
              }
            }

            ServiceComponentHostResponse r = sch.convertToResponse(desiredConfigs);
            if (null == r || (filterBasedConfigStaleness && r.isStaleConfig() != staleConfig)) {
              continue;
            }

            Host host = hosts.get(sch.getHostName());
            if (host == null) {
              throw new HostNotFoundException(cluster.getClusterName(), sch.getHostName());
            }

            MaintenanceState effectiveMaintenanceState = maintenanceStateHelper.getEffectiveState(sch, host);
            if(filterByMaintenanceState(request, effectiveMaintenanceState)) {
              continue;
            }
            r.setMaintenanceState(effectiveMaintenanceState.name());

            response.add(r);
          } catch (ServiceComponentHostNotFoundException e) {
            if (request.getServiceName() == null || request.getComponentName() == null) {
              // Ignore the exception if either the service name or component name are not specified.
              // This is an artifact of how we get host_components and can happen in the case where
              // we get all host_components for a host, for example.
              LOG.debug("Ignoring not specified host_component ", e);

            } else {
              // Otherwise rethrow the exception and let the caller decide if it's an error condition.
              // Logging the exception as debug since this does not necessarily indicate an error
              // condition.
              LOG.debug("ServiceComponentHost not found ", e);
              throw new ServiceComponentHostNotFoundException(cluster.getClusterName(),
                  request.getServiceName(), request.getComponentName(), request.getHostname());
            }
          }
        } else {
          for (ServiceComponentHost sch : serviceComponentHostMap.values()) {
            if (null == sch) {
              // It's possible that the host was deleted during the time that the request was generated.
              continue;
            }

            if (checkDesiredState && (desiredStateToCheck != sch.getDesiredState())) {
              continue;
            }

            if (checkState && stateToCheck != sch.getState()) {
              continue;
            }

            if (request.getAdminState() != null) {
              String stringToMatch =
                  sch.getComponentAdminState() == null ? "" : sch.getComponentAdminState().name();
              if (!request.getAdminState().equals(stringToMatch)) {
                continue;
              }
            }

            ServiceComponentHostResponse r = sch.convertToResponse(desiredConfigs);
            if (null == r || (filterBasedConfigStaleness && r.isStaleConfig() != staleConfig)) {
              continue;
            }

            Host host = hosts.get(sch.getHostName());
            if (host == null) {
              throw new HostNotFoundException(cluster.getClusterName(), sch.getHostName());
            }

            MaintenanceState effectiveMaintenanceState = maintenanceStateHelper.getEffectiveState(sch, host);
            if(filterByMaintenanceState(request, effectiveMaintenanceState)) {
              continue;
            }
            r.setMaintenanceState(effectiveMaintenanceState.name());

            response.add(r);
          }
        }
      }
    }
    return response;
  }

  private boolean filterByMaintenanceState(ServiceComponentHostRequest request, MaintenanceState effectiveMaintenanceState) {
    if (request.getMaintenanceState() != null) {
      MaintenanceState desiredMaintenanceState = MaintenanceState.valueOf(request.getMaintenanceState());
      if (desiredMaintenanceState.equals(MaintenanceState.ON)) {
        /*
         * if we want components with ON state it can be one of IMPLIED_FROM_SERVICE,
         * IMPLIED_FROM_SERVICE_AND_HOST, IMPLIED_FROM_HOST, ON, ro simply - not OFF
         */
        if (effectiveMaintenanceState.equals(MaintenanceState.OFF)) {
          return true;
        }
      } else if (!desiredMaintenanceState.equals(effectiveMaintenanceState)){
        return true;
      }
    }
    return false;
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
            cluster.getClusterName(), config);
        responses.add(response);
      }
    }
    else {
      boolean includeProps = request.includeProperties();
      if (null != request.getType()) {
        Map<String, Config> configs = cluster.getConfigsByType(
            request.getType());

        if (null != configs) {
          for (Entry<String, Config> entry : configs.entrySet()) {
            Config config = entry.getValue();
            ConfigurationResponse response = new ConfigurationResponse(
                cluster.getClusterName(), config.getStackId(),
                request.getType(),
                config.getTag(), entry.getValue().getVersion(),
                includeProps ? config.getProperties() : new HashMap<String, String>(),
                includeProps ? config.getPropertiesAttributes() : new HashMap<String, Map<String,String>>(),
                config.getPropertiesTypes());
            responses.add(response);
          }
        }
      } else {
        // !!! all configuration
        Collection<Config> all = cluster.getAllConfigs();

        for (Config config : all) {
          ConfigurationResponse response = new ConfigurationResponse(
              cluster.getClusterName(), config.getStackId(), config.getType(),
              config.getTag(), config.getVersion(),
              includeProps ? config.getProperties() : new HashMap<String, String>(),
              includeProps ? config.getPropertiesAttributes() : new HashMap<String, Map<String,String>>(),
              config.getPropertiesTypes());

          responses.add(response);
        }
      }
    }

    return responses;

  }

  @Override
  public synchronized RequestStatusResponse updateClusters(Set<ClusterRequest> requests,
                                                           Map<String, String> requestProperties)
      throws AmbariException, AuthorizationException {

    RequestStatusResponse response = null;

    // We have to allow for multiple requests to account for multiple
    // configuration updates (create multiple configuration resources)...
    for (ClusterRequest request : requests) {
      // TODO : Is there ever a real world case where we could have multiple non-null responses?

      // ***************************************************
      // set any session attributes for this cluster request
      Cluster cluster;
      if (request.getClusterId() == null) {
        cluster = clusters.getCluster(request.getClusterName());
      } else {
        cluster = clusters.getClusterById(request.getClusterId());
      }

      if (cluster == null) {
        throw new AmbariException("The cluster may not be null");
      }

      Map<String, Object> sessionAttributes = request.getSessionAttributes();

      // TODO: Once the UI uses the Credential Resource API, remove this block to _clean_ the
      // TODO: session attributes and store any KDC administrator credentials in the secure
      // TODO: credential provider facility.
      // For now, to keep things backwards compatible, get and remove the KDC administrator credentials
      // from the session attributes and store them in the CredentialsProvider. The KDC administrator
      // credentials are prefixed with kdc_admin/. The following attributes are expected, if setting
      // the KDC administrator credentials:
      //    kerberos_admin/principal
      //    kerberos_admin/password
      if((sessionAttributes != null) && !sessionAttributes.isEmpty()) {
        Map<String, Object> cleanedSessionAttributes = new HashMap<String, Object>();
        String principal = null;
        char[] password = null;

        for(Map.Entry<String,Object> entry: sessionAttributes.entrySet()) {
          String name = entry.getKey();
          Object value = entry.getValue();

          if ("kerberos_admin/principal".equals(name)) {
            if(value instanceof String) {
              principal = (String)value;
            }
          }
          else if ("kerberos_admin/password".equals(name)) {
            if(value instanceof String) {
              password = ((String) value).toCharArray();
            }
          } else {
            cleanedSessionAttributes.put(name, value);
          }
        }

        if(principal != null) {
          // The KDC admin principal exists... set the credentials in the credentials store
          credentialStoreService.setCredential(cluster.getClusterName(),
              KerberosHelper.KDC_ADMINISTRATOR_CREDENTIAL_ALIAS,
              new PrincipalKeyCredential(principal, password), CredentialStoreType.TEMPORARY);
        }

        sessionAttributes = cleanedSessionAttributes;
      }
      // TODO: END

      cluster.addSessionAttributes(sessionAttributes);
      //
      // ***************************************************

      response = updateCluster(request, requestProperties);
    }
    return response;
  }

  private synchronized RequestStatusResponse updateCluster(ClusterRequest request, Map<String, String> requestProperties)
      throws AmbariException, AuthorizationException {

    RequestStageContainer requestStageContainer = null;

    if (request.getClusterId() == null
        && (request.getClusterName() == null
        || request.getClusterName().isEmpty())) {
      throw new IllegalArgumentException("Invalid arguments, cluster id or cluster name should not be null");
    }

    LOG.info("Received a updateCluster request"
        + ", clusterId=" + request.getClusterId()
        + ", clusterName=" + request.getClusterName()
        + ", securityType=" + request.getSecurityType()
        + ", request=" + request);

    final Cluster cluster;
    if (request.getClusterId() == null) {
      cluster = clusters.getCluster(request.getClusterName());
    } else {
      cluster = clusters.getClusterById(request.getClusterId());
    }

    // Ensure the user has access to update this cluster
    AuthorizationHelper.verifyAuthorization(ResourceType.CLUSTER, cluster.getResourceId(), RoleAuthorization.AUTHORIZATIONS_UPDATE_CLUSTER);

    //save data to return configurations created
    List<ConfigurationResponse> configurationResponses =
      new LinkedList<ConfigurationResponse>();
    ServiceConfigVersionResponse serviceConfigVersionResponse = null;

    if (request.getDesiredConfig() != null && request.getServiceConfigVersionRequest() != null) {
      String msg = "Unable to set desired configs and rollback at same time, request = " + request.toString();
      LOG.error(msg);
      throw new IllegalArgumentException(msg);
    }

    // set the new name of the cluster if change is requested
    if (!cluster.getClusterName().equals(request.getClusterName())) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Received cluster name change request from " + cluster.getClusterName() + " to " + request.getClusterName());
      }

      if(!AuthorizationHelper.isAuthorized(ResourceType.AMBARI, null, EnumSet.of(RoleAuthorization.AMBARI_RENAME_CLUSTER))) {
        throw new AuthorizationException("The authenticated user does not have authorization to rename the cluster");
      }

      cluster.setClusterName(request.getClusterName());
    }

    //check if desired configs are available in request and they were changed
    boolean isConfigurationCreationNeeded = false;
    if (request.getDesiredConfig() != null) {
      for (ConfigurationRequest desiredConfig : request.getDesiredConfig()) {
        Map<String, String> requestConfigProperties = desiredConfig.getProperties();
        Map<String,Map<String,String>> requestConfigAttributes = desiredConfig.getPropertiesAttributes();

        // processing password properties
        if(requestConfigProperties != null && !requestConfigProperties.isEmpty()) {
          Map<PropertyInfo.PropertyType, Set<String>> propertiesTypes = cluster.getConfigPropertiesTypes(
              desiredConfig.getType()
          );
          for (Entry<String, String> property : requestConfigProperties.entrySet()) {
            String propertyName = property.getKey();
            String propertyValue = property.getValue();
            if ((propertiesTypes.containsKey(PropertyType.PASSWORD) &&
                propertiesTypes.get(PropertyType.PASSWORD).contains(propertyName)) ||
                (requestConfigAttributes != null && requestConfigAttributes.containsKey(PASSWORD) &&
                requestConfigAttributes.get(PASSWORD).containsKey(propertyName) &&
                requestConfigAttributes.get(PASSWORD).get(propertyName).equals("true"))) {
              if (SecretReference.isSecret(propertyValue)) {
                SecretReference ref = new SecretReference(propertyValue, cluster);
                requestConfigProperties.put(propertyName, ref.getValue());
              }
            }
          }
        }

        Config clusterConfig = cluster.getDesiredConfigByType(desiredConfig.getType());
        Map<String, String> clusterConfigProperties = null;
        Map<String,Map<String,String>> clusterConfigAttributes = null;
        if (clusterConfig != null) {
          clusterConfigProperties = clusterConfig.getProperties();
          clusterConfigAttributes = clusterConfig.getPropertiesAttributes();
          if (!isAttributeMapsEqual(requestConfigAttributes, clusterConfigAttributes)){
            isConfigurationCreationNeeded = true;
            break;
          }
        } else {
          isConfigurationCreationNeeded = true;
          break;
        }

        if (requestConfigProperties == null || requestConfigProperties.isEmpty()) {
          Config existingConfig = cluster.getConfig(desiredConfig.getType(), desiredConfig.getVersionTag());
          if (existingConfig != null) {
            if (!StringUtils.equals(existingConfig.getTag(), clusterConfig.getTag())) {
              isConfigurationCreationNeeded = true;
              break;
            }
          }
        }
        if (requestConfigProperties != null && clusterConfigProperties != null) {
          if (requestConfigProperties.size() != clusterConfigProperties.size()) {
            isConfigurationCreationNeeded = true;
            break;
          } else {
            if ( cluster.getServiceByConfigType(clusterConfig.getType()) != null &&  clusterConfig.getServiceConfigVersions().isEmpty() ) {

              //If there's no service config versions containing this config (except cluster configs), recreate it even if exactly equal
              LOG.warn("Existing desired config doesn't belong to any service config version, " +
                  "forcing config recreation, " +
                  "clusterName={}, type = {}, tag={}", cluster.getClusterName(), clusterConfig.getType(),
                  clusterConfig.getTag());
              isConfigurationCreationNeeded = true;
              break;
            }
            for (Entry<String, String> property : requestConfigProperties.entrySet()) {
              if (!StringUtils.equals(property.getValue(), clusterConfigProperties.get(property.getKey()))) {
                isConfigurationCreationNeeded = true;
                break;
              }
            }
          }
        }
      }
    }

    // set or create configuration mapping (and optionally create the map of properties)
    if (isConfigurationCreationNeeded) {
      List<ConfigurationRequest> desiredConfigs = request.getDesiredConfig();

      if (!desiredConfigs.isEmpty()) {
        Set<Config> configs = new HashSet<Config>();
        String note = null;

        for (ConfigurationRequest cr : desiredConfigs) {
          String configType = cr.getType();

          if (null != cr.getProperties()) {
            // !!! empty property sets are supported, and need to be able to use
            // previously-defined configs (revert)
            Map<String, Config> all = cluster.getConfigsByType(configType);
            if (null == all ||                              // none set
                !all.containsKey(cr.getVersionTag()) ||     // tag not set
                cr.getProperties().size() > 0) {            // properties to set

              cr.setClusterName(cluster.getClusterName());
              configurationResponses.add(createConfiguration(cr));

              LOG.info(MessageFormat.format("Applying configuration with tag ''{0}'' to cluster ''{1}''  for configuration type {2}",
                  cr.getVersionTag(),
                  request.getClusterName(),
                  configType));
            }
          }
          note = cr.getServiceConfigVersionNote();
          configs.add(cluster.getConfig(configType, cr.getVersionTag()));
        }
        if (!configs.isEmpty()) {
          String authName = getAuthName();
          serviceConfigVersionResponse = cluster.addDesiredConfig(authName, configs, note);
          if (serviceConfigVersionResponse != null) {
            Logger logger = LoggerFactory.getLogger("configchange");
            for (Config config : configs) {
              logger.info("cluster '" + request.getClusterName() + "' "
                  + "changed by: '" + authName + "'; "
                  + "type='" + config.getType() + "' "
                  + "tag='" + config.getTag() + "'");
            }
          }
        }
      }
    }

    StackId currentVersion = cluster.getCurrentStackVersion();
    StackId desiredVersion = cluster.getDesiredStackVersion();

    // Set the current version value if its not already set
    if (currentVersion == null) {
      if(!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, cluster.getResourceId(), EnumSet.of(RoleAuthorization.CLUSTER_UPGRADE_DOWNGRADE_STACK))) {
        throw new AuthorizationException("The authenticated user does not have authorization to modify stack version");
      }

      cluster.setCurrentStackVersion(desiredVersion);
    }
    // Stack Upgrade: unlike the workflow for creating a cluster, updating a cluster via the API will not
    // create any ClusterVersionEntity changes because those have to go through the Stack Upgrade process.

    boolean requiresHostListUpdate =
        request.getHostNames() != null && !request.getHostNames().isEmpty();

    if (requiresHostListUpdate) {
      clusters.mapAndPublishHostsToCluster(
          request.getHostNames(), request.getClusterName());
    }

    // set the provisioning state of the cluster
    if (null != request.getProvisioningState()) {
      State oldProvisioningState = cluster.getProvisioningState();
      State provisioningState = State.valueOf(request.getProvisioningState());

      if (provisioningState != State.INIT
          && provisioningState != State.INSTALLED) {
        LOG.warn(
            "Invalid cluster provisioning state {} cannot be set on the cluster {}",
            provisioningState, request.getClusterName());

        throw new IllegalArgumentException(
            "Invalid cluster provisioning state "
            + provisioningState + " cannot be set on cluster "
            + request.getClusterName());
      }

      if (provisioningState != oldProvisioningState) {
        boolean isStateTransitionValid = State.isValidDesiredStateTransition(
            oldProvisioningState, provisioningState);

        if (!isStateTransitionValid) {
          LOG.warn(
              "Invalid cluster provisioning 2state {} cannot be set on the cluster {} because the current state is {}",
              provisioningState, request.getClusterName(), oldProvisioningState);

          throw new AmbariException("Invalid transition for"
              + " cluster provisioning state" + ", clusterName="
              + cluster.getClusterName() + ", clusterId="
              + cluster.getClusterId() + ", currentProvisioningState="
              + oldProvisioningState + ", newProvisioningState="
              + provisioningState);
        }
      }

      cluster.setProvisioningState(provisioningState);
    }

    if (null != request.getServiceConfigVersionRequest()) {
      if(!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, cluster.getResourceId(), EnumSet.of(RoleAuthorization.SERVICE_MODIFY_CONFIGS))) {
        throw new AuthorizationException("The authenticated user does not have authorization to modify service configurations");
      }

      ServiceConfigVersionRequest serviceConfigVersionRequest = request.getServiceConfigVersionRequest();
      if (StringUtils.isEmpty(serviceConfigVersionRequest.getServiceName()) ||
          null == serviceConfigVersionRequest.getVersion()) {
        String msg = "Service name and version should be specified in service config version";
        LOG.error(msg);
        throw new IllegalArgumentException(msg);
      }

      serviceConfigVersionResponse = cluster.setServiceConfigVersion(serviceConfigVersionRequest.getServiceName(),
          serviceConfigVersionRequest.getVersion(), getAuthName(),
          serviceConfigVersionRequest.getNote());
    }

    if (serviceConfigVersionResponse != null) {
      if (!configurationResponses.isEmpty()) {
        serviceConfigVersionResponse.setConfigurations(configurationResponses);
      }

      ClusterResponse clusterResponse =
          new ClusterResponse(cluster.getClusterId(), cluster.getClusterName(), null, null, null, null, null, null);

      Map<String, Collection<ServiceConfigVersionResponse>> map =
        new HashMap<String, Collection<ServiceConfigVersionResponse>>();
      map.put(serviceConfigVersionResponse.getServiceName(), Collections.singletonList(serviceConfigVersionResponse));

      clusterResponse.setDesiredServiceConfigVersions(map);

      //workaround to be able to retrieve update results in resource provider
      //as this method only expected to return request response
      saveClusterUpdate(request, clusterResponse);
    }

    // set the new security type of the cluster if change is requested
    SecurityType securityType = request.getSecurityType();

    if(securityType != null) {
      // if any custom operations are valid and requested, the process of executing them should be initiated,
      // most of the validation logic will be left to the KerberosHelper to avoid polluting the controller
      if (kerberosHelper.shouldExecuteCustomOperations(securityType, requestProperties)) {
        if(!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, cluster.getResourceId(), EnumSet.of(RoleAuthorization.CLUSTER_TOGGLE_KERBEROS))) {
          throw new AuthorizationException("The authenticated user does not have authorization to perform Kerberos-specific operations");
        }

        try {
          requestStageContainer = kerberosHelper.executeCustomOperations(cluster, requestProperties, requestStageContainer,
              kerberosHelper.getManageIdentitiesDirective(requestProperties));
        } catch (KerberosOperationException e) {
          throw new IllegalArgumentException(e.getMessage(), e);
        }
      } else {
        // If force_toggle_kerberos is not specified, null will be returned. Therefore, perform an
        // equals check to yield true if the result is Boolean.TRUE, otherwise false.
        boolean forceToggleKerberos = kerberosHelper.getForceToggleKerberosDirective(requestProperties);

        if (forceToggleKerberos || (cluster.getSecurityType() != securityType)) {
          LOG.info("Received cluster security type change request from {} to {} (forced: {})",
              cluster.getSecurityType().name(), securityType.name(), forceToggleKerberos);

          if ((securityType == SecurityType.KERBEROS) || (securityType == SecurityType.NONE)) {
            if (!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, cluster.getResourceId(), EnumSet.of(RoleAuthorization.CLUSTER_TOGGLE_KERBEROS))) {
              throw new AuthorizationException("The authenticated user does not have authorization to enable or disable Kerberos");
            }

            // Since the security state of the cluster has changed, invoke toggleKerberos to handle
            // adding or removing Kerberos from the cluster. This may generate multiple stages
            // or not depending the current state of the cluster.
            try {
              requestStageContainer = kerberosHelper.toggleKerberos(cluster, securityType, requestStageContainer,
                  kerberosHelper.getManageIdentitiesDirective(requestProperties));
            } catch (KerberosOperationException e) {
              throw new IllegalArgumentException(e.getMessage(), e);
            }
          } else {
            throw new IllegalArgumentException(String.format("Unexpected security type encountered: %s", securityType.name()));
          }

          cluster.setSecurityType(securityType);
        }
      }
    }

    if (requestStageContainer != null) {
      requestStageContainer.persist();
      return requestStageContainer.getRequestStatusResponse();
    } else {
      return null;
    }
  }

  /**
   * Given a configuration request, compares the requested properties to the current set of desired
   * properties for the same configuration type and returns a map of property names to an array of
   * Strings representing the current value (index 0), and the requested value (index 1).
   * <p>
   * <ul>
   * <li>
   * If a property is set in the requested property set and not found in the current property set,
   * the current value (index 0) will be <code>null</code> - {<code>null</code>, "requested value"}
   * </li>
   * <li>
   * If a property is set in the current property set and not found in the requested property set,
   * the requested value (index 1) will be <code>null</code> - {"current value", <code>null</code>}
   * </li>
   * <li>
   * If a property found in bother current property set and the requested property set,
   * the requested value (index 1) will be <code>null</code> - {"current value", "requested value"}
   * </li>
   * </ul>
   *
   * @param cluster the relevant cluster
   * @param request the request data
   * @return a map lf property names to String arrays indicating the requsted changes ({current value, requested valiue})
   */
  private Map<String, String[]> getPropertyChanges(Cluster cluster, ConfigurationRequest request) {
    Map<String, String[]>  changedProperties = new HashMap<String, String[]>();

    // Ensure that the requested property map is not null.
    Map<String, String> requestedProperties  = request.getProperties();
    if (requestedProperties == null) {
      requestedProperties = Collections.emptyMap();
    }

    // Get the current/desired properties for the relevant configuration type and ensure that the
    // property map is not null.
    Config existingConfig = cluster.getDesiredConfigByType(request.getType());
    Map<String, String> existingProperties = (existingConfig == null) ? null : existingConfig.getProperties();
    if (existingProperties == null) {
      existingProperties = Collections.emptyMap();
    }

    // Ensure all propery names are captured, including missing ones from either set.
    Set<String> propertyNames = new HashSet<String>();
    propertyNames.addAll(requestedProperties.keySet());
    propertyNames.addAll(existingProperties.keySet());

    for(String propertyName:propertyNames) {
      String requestedValue = requestedProperties.get(propertyName);
      String existingValue = existingProperties.get(propertyName);

      // Perform case-sensitive match.  It is possible that case matters here.
      if((requestedValue == null) ? (existingValue != null) : !requestedValue.equals(existingValue)) {
        changedProperties.put(propertyName, new String[]{existingValue, requestedValue});
      }
    }

    return changedProperties;
  }

  /**
   * Comparison of two attributes maps
   * @param requestConfigAttributes - attribute map sent from API
   * @param clusterConfigAttributes - existed attribute map
   * @return true if maps is equal (have the same attributes and their values)
   */
  public boolean isAttributeMapsEqual(Map<String, Map<String, String>> requestConfigAttributes,
          Map<String, Map<String, String>> clusterConfigAttributes) {
    boolean isAttributesEqual = true;
    if ((requestConfigAttributes != null && clusterConfigAttributes == null)
            || (requestConfigAttributes == null && clusterConfigAttributes != null)
            || (requestConfigAttributes != null && clusterConfigAttributes != null
            && !requestConfigAttributes.keySet().equals(clusterConfigAttributes.keySet()))) {
      return false;
    } else if (clusterConfigAttributes != null && requestConfigAttributes != null) {
      for (Entry<String, Map<String, String>> ClusterEntrySet : clusterConfigAttributes.entrySet()) {
        Map<String, String> clusterMapAttributes = ClusterEntrySet.getValue();
        Map<String, String> requestMapAttributes = requestConfigAttributes.get(ClusterEntrySet.getKey());
        if ((requestMapAttributes != null && clusterMapAttributes == null)
                || (requestMapAttributes == null && clusterMapAttributes != null)
                || (requestMapAttributes != null && clusterMapAttributes != null
                && !requestMapAttributes.keySet().equals(clusterMapAttributes.keySet()))) {
          return false;
        } else if (requestMapAttributes != null && clusterMapAttributes != null) {
          for (Entry<String, String> requestPropertyEntrySet : requestMapAttributes.entrySet()) {
            String requestPropertyValue = requestPropertyEntrySet.getValue();
            String clusterPropertyValue = clusterMapAttributes.get(requestPropertyEntrySet.getKey());
            if ((requestPropertyValue != null && clusterPropertyValue == null)
                    || (requestPropertyValue == null && clusterPropertyValue != null)
                    || (requestPropertyValue != null && clusterPropertyValue != null
                    && !requestPropertyValue.equals(clusterPropertyValue))) {
              return false;
            }
          }
        }

      }
    }
    return isAttributesEqual;
  }

  /**
   * Save cluster update results to retrieve later
   * @param clusterRequest   cluster request info
   * @param clusterResponse  cluster response info
   */
  public void saveClusterUpdate(ClusterRequest clusterRequest, ClusterResponse clusterResponse) {
    clusterUpdateCache.put(clusterRequest, clusterResponse);
  }


  @Override
  public ClusterResponse getClusterUpdateResults(ClusterRequest clusterRequest) {
    return clusterUpdateCache.getIfPresent(clusterRequest);
  }

  @Override
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

    // We choose the most general (high-level) op level here. As a result,
    // service checks will be only launched for services/components that
    // are not in a Maintenance state.
    Resource.Type opLvl = Resource.Type.Cluster;

    Set<String> smokeTestServices = new HashSet<String>();

    // Adding smoke checks for changed services
    if (changedServices != null) {
      for (Entry<State, List<Service>> entry : changedServices.entrySet()) {
        if (State.STARTED != entry.getKey()) {
          continue;
        }
        for (Service s : entry.getValue()) {
          if (runSmokeTest && (State.INSTALLED == s.getDesiredState() &&
                  maintenanceStateHelper.isOperationAllowed(opLvl, s))) {
            smokeTestServices.add(s.getName());
          }
        }
      }
    }

    // Adding smoke checks for changed host components
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
          if (! maintenanceStateHelper.isOperationAllowed(opLvl, sch)) {
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

    // Add service checks for any changed master component hosts or if
    // more then one component has been changed for a service
    for (Entry<String, Map<String, Integer>> entry :
      changedComponentCount.entrySet()) {
      String serviceName = entry.getKey();
      Service s = cluster.getService(serviceName);
      // smoke test service if more than one component is started
      if (runSmokeTest && (entry.getValue().size() > 1) &&
              maintenanceStateHelper.isOperationAllowed(opLvl, s)) {
        smokeTestServices.add(serviceName);
        continue;
      }
      for (String componentName :
        changedComponentCount.get(serviceName).keySet()) {
        ServiceComponent sc = cluster.getService(serviceName).
          getServiceComponent(componentName);
        StackId stackId = sc.getDesiredStackVersion();
        ComponentInfo compInfo = ambariMetaInfo.getComponent(
          stackId.getStackName(), stackId.getStackVersion(), serviceName,
          componentName);
        if (runSmokeTest && compInfo.isMaster() &&
                // op lvl handling for service component
                // is the same as for service
                maintenanceStateHelper.isOperationAllowed(opLvl, s)) {
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
      for (Entry<String, Map<State, List<ServiceComponentHost>>> stringMapEntry : changedScHosts.entrySet()) {
        for (State state : stringMapEntry.getValue().keySet()) {
          if (state == State.STARTED) {
            serviceComponentHosts.addAll(stringMapEntry.getValue().get(state));
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
      for (Entry<String, List<ServiceComponentHost>> stringListEntry : clientSchs.entrySet()) {
        Map<State, List<ServiceComponentHost>> schMap = new EnumMap<State, List<ServiceComponentHost>>(State.class);
        schMap.put(State.INSTALLED, stringListEntry.getValue());
        changedScHosts.put(stringListEntry.getKey(), schMap);
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
                                Stage stage,
                                ServiceComponentHost scHost,
                                Map<String, Map<String, String>> configurations,
                                Map<String, Map<String, Map<String, String>>> configurationAttributes,
                                Map<String, Map<String, String>> configTags,
                                RoleCommand roleCommand,
                                Map<String, String> commandParamsInp,
                                ServiceComponentHostEvent event,
                                boolean skipFailure
                                )
                                throws AmbariException {

    String serviceName = scHost.getServiceName();

    stage.addHostRoleExecutionCommand(scHost.getHost(),
        Role.valueOf(scHost.getServiceComponentName()), roleCommand, event, cluster, serviceName, false, skipFailure);

    String componentName = scHost.getServiceComponentName();
    String hostname = scHost.getHostName();
    String osFamily = clusters.getHost(hostname).getOsFamily();
    StackId stackId = cluster.getDesiredStackVersion();
    ServiceInfo serviceInfo = ambariMetaInfo.getService(stackId.getStackName(),
        stackId.getStackVersion(), serviceName);
    ComponentInfo componentInfo = ambariMetaInfo.getComponent(
      stackId.getStackName(), stackId.getStackVersion(),
      serviceName, componentName);
    StackInfo stackInfo = ambariMetaInfo.getStack(stackId.getStackName(),
        stackId.getStackVersion());

    ExecutionCommand execCmd = stage.getExecutionCommandWrapper(scHost.getHostName(),
      scHost.getServiceComponentName()).getExecutionCommand();

    Host host = clusters.getHost(scHost.getHostName());

    execCmd.setConfigurations(configurations);
    execCmd.setConfigurationAttributes(configurationAttributes);
    execCmd.setConfigurationTags(configTags);

    // Get the value of credential store enabled from the DB
    Service clusterService = cluster.getService(serviceName);
    execCmd.setCredentialStoreEnabled(String.valueOf(clusterService.isCredentialStoreEnabled()));

    // Get the map of service config type to password properties for the service
    Map<String, Map<String, String>> configCredentials;
    configCredentials = configCredentialsForService.get(clusterService.getName());
    if (configCredentials == null) {
      configCredentials = configHelper.getPropertiesWithPropertyType(stackId, clusterService,
              PropertyType.PASSWORD);
      configCredentialsForService.put(clusterService.getName(), configCredentials);
    }

    execCmd.setConfigurationCredentials(configCredentials);

    // Create a local copy for each command
    Map<String, String> commandParams = new TreeMap<String, String>();
    if (commandParamsInp != null) { // if not defined
      commandParams.putAll(commandParamsInp);
    }

    // Propagate HCFS service type info
    for (Service service : cluster.getServices().values()) {
      ServiceInfo serviceInfoInstance = ambariMetaInfo.getService(stackId.getStackName(),stackId.getStackVersion(), service.getName());
      LOG.debug("Iterating service type Instance in createHostAction: {}", serviceInfoInstance.getName());
      String serviceType = serviceInfoInstance.getServiceType();
      if (serviceType != null) {
        LOG.info("Adding service type info in createHostAction: {}", serviceType);
        commandParams.put("dfs_type", serviceType);
        break;
      }
    }

    boolean isInstallCommand = roleCommand.equals(RoleCommand.INSTALL);
    String agentDefaultCommandTimeout = configs.getDefaultAgentTaskTimeout(isInstallCommand);
    String scriptCommandTimeout = "";
    /*
     * This script is only used for
     * default commands like INSTALL/STOP/START
     */
    CommandScriptDefinition script = componentInfo.getCommandScript();
    if (serviceInfo.getSchemaVersion().equals(AmbariMetaInfo.SCHEMA_VERSION_2)) {
      if (script != null) {
        commandParams.put(SCRIPT, script.getScript());
        commandParams.put(SCRIPT_TYPE, script.getScriptType().toString());

        boolean retryEnabled = false;
        Integer retryMaxTime = 0;
        if (commandParams.containsKey(CLUSTER_PHASE_PROPERTY) &&
            (commandParams.get(CLUSTER_PHASE_PROPERTY).equals(CLUSTER_PHASE_INITIAL_INSTALL) ||
            commandParams.get(CLUSTER_PHASE_PROPERTY).equals(CLUSTER_PHASE_INITIAL_START))) {
          String retryEnabledStr =
              configHelper.getValueFromDesiredConfigurations(cluster, ConfigHelper.CLUSTER_ENV,
                                                             ConfigHelper.CLUSTER_ENV_RETRY_ENABLED);
          String commandsStr =
              configHelper.getValueFromDesiredConfigurations(cluster, ConfigHelper.CLUSTER_ENV,
                                                             ConfigHelper.CLUSTER_ENV_RETRY_COMMANDS);
          String retryMaxTimeStr =
              configHelper.getValueFromDesiredConfigurations(cluster,
                                                             ConfigHelper.CLUSTER_ENV,
                                                             ConfigHelper.CLUSTER_ENV_RETRY_MAX_TIME_IN_SEC);
          if (StringUtils.isNotEmpty(retryEnabledStr)) {
            retryEnabled = Boolean.TRUE.toString().equals(retryEnabledStr);
          }

          if (retryEnabled) {
            retryMaxTime = NumberUtils.toInt(retryMaxTimeStr, 0);
            if (retryMaxTime < 0) {
              retryMaxTime = 0;
            }

            if (StringUtils.isNotEmpty(commandsStr)) {
              boolean commandMayBeRetried = false;
              String[] commands = commandsStr.split(",");
              for (String command : commands) {
                if (roleCommand.toString().equals(command.trim())) {
                  commandMayBeRetried = true;
                }
              }
              retryEnabled = commandMayBeRetried;
            }
          }
          LOG.info("Auto retry setting for {}-{} on {} is retryEnabled={} and retryMaxTime={}", serviceName, componentName, scHost.getHostName(), retryEnabled, retryMaxTime);
        }
        commandParams.put(MAX_DURATION_OF_RETRIES, Integer.toString(retryMaxTime));
        commandParams.put(COMMAND_RETRY_ENABLED, Boolean.toString(retryEnabled));

        ClusterVersionEntity effectiveClusterVersion = cluster.getEffectiveClusterVersion();
        if (effectiveClusterVersion != null) {
         commandParams.put(VERSION, effectiveClusterVersion.getRepositoryVersion().getVersion());
        }
        if (script.getTimeout() > 0) {
          scriptCommandTimeout = String.valueOf(script.getTimeout());
        }
      } else {
        String message = String.format("Component %s of service %s has no " +
          "command script defined", componentName, serviceName);
        throw new AmbariException(message);
      }
    }

    String actualTimeout = (!scriptCommandTimeout.equals("") ? scriptCommandTimeout : agentDefaultCommandTimeout);

    // Because the INSTALL command can take much longer than typical commands, set the timeout to be the max
    // between the script's service component timeout and the agent default timeout.
    if (roleCommand.equals(RoleCommand.INSTALL) && !agentDefaultCommandTimeout.equals("") &&
        Integer.parseInt(actualTimeout) < Integer.parseInt(agentDefaultCommandTimeout)) {
      actualTimeout = agentDefaultCommandTimeout;
    }

    commandParams.put(COMMAND_TIMEOUT, actualTimeout);
    commandParams.put(SERVICE_PACKAGE_FOLDER,
      serviceInfo.getServicePackageFolder());
    commandParams.put(HOOKS_FOLDER, stackInfo.getStackHooksFolder());

    String customCacheDirectory = componentInfo.getCustomFolder();
    if (customCacheDirectory != null) {
      File customCache = new File(configs.getResourceDirPath(), customCacheDirectory);
      if (customCache.exists() && customCache.isDirectory()) {
        commandParams.put(CUSTOM_FOLDER, customCacheDirectory);
      }
    }

    String clusterName = cluster.getClusterName();
    if (customCommandExecutionHelper.isTopologyRefreshRequired(roleCommand.name(), clusterName, serviceName)) {
      commandParams.put(ExecutionCommand.KeyNames.REFRESH_TOPOLOGY, "True");
    }

    execCmd.setCommandParams(commandParams);

    String repoInfo = customCommandExecutionHelper.getRepoInfo(cluster, host);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Sending repo information to agent"
        + ", hostname=" + scHost.getHostName()
        + ", clusterName=" + clusterName
        + ", stackInfo=" + stackId.getStackId()
        + ", repoInfo=" + repoInfo);
    }

    Map<String, String> hostParams = new TreeMap<String, String>();
    hostParams.put(REPO_INFO, repoInfo);
    hostParams.putAll(getRcaParameters());

    // use the effective cluster version here since this command might happen
    // in the context of an upgrade and we should send the repo ID which matches
    // the version being send down
    RepositoryVersionEntity repoVersion = null;
    ClusterVersionEntity effectiveClusterVersion = cluster.getEffectiveClusterVersion();
    if (null != effectiveClusterVersion) {
      repoVersion = effectiveClusterVersion.getRepositoryVersion();
    } else {
      List<ClusterVersionEntity> list = clusterVersionDAO.findByClusterAndState(cluster.getClusterName(),
          RepositoryVersionState.INIT);
      if (1 == list.size()) {
        repoVersion = list.get(0).getRepositoryVersion();
      }
    }

    if (null != repoVersion) {
      try {
        VersionDefinitionXml xml = repoVersion.getRepositoryXml();
        if (null != xml && !StringUtils.isBlank(xml.getPackageVersion(osFamily))) {
          hostParams.put(PACKAGE_VERSION, xml.getPackageVersion(osFamily));
        }
      } catch (Exception e) {
        throw new AmbariException(String.format("Could not load version xml from repo version %s",
            repoVersion.getVersion()), e);
      }

      hostParams.put(KeyNames.REPO_VERSION_ID, repoVersion.getId().toString());
    }

    List<ServiceOsSpecific.Package> packages =
            getPackagesForServiceHost(serviceInfo, hostParams, osFamily);
    String packageList = gson.toJson(packages);
    hostParams.put(PACKAGE_LIST, packageList);

    Map<String, DesiredConfig> desiredConfigs = cluster.getDesiredConfigs();

    Set<String> userSet = configHelper.getPropertyValuesWithPropertyType(stackId, PropertyType.USER, cluster, desiredConfigs);
    String userList = gson.toJson(userSet);
    hostParams.put(USER_LIST, userList);

    Set<String> groupSet = configHelper.getPropertyValuesWithPropertyType(stackId, PropertyType.GROUP, cluster, desiredConfigs);
    String groupList = gson.toJson(groupSet);
    hostParams.put(GROUP_LIST, groupList);

    Set<String> notManagedHdfsPathSet = configHelper.getPropertyValuesWithPropertyType(stackId, PropertyType.NOT_MANAGED_HDFS_PATH, cluster, desiredConfigs);
    String notManagedHdfsPathList = gson.toJson(notManagedHdfsPathSet);
    hostParams.put(NOT_MANAGED_HDFS_PATH_LIST, notManagedHdfsPathList);

    DatabaseType databaseType = configs.getDatabaseType();
    if (databaseType == DatabaseType.ORACLE) {
      hostParams.put(DB_DRIVER_FILENAME, configs.getOjdbcJarName());
    } else if (databaseType == DatabaseType.MYSQL) {
      hostParams.put(DB_DRIVER_FILENAME, configs.getMySQLJarName());
    }

    List<String> clientsToUpdateConfigsList = componentInfo.getClientsToUpdateConfigs();
    if (clientsToUpdateConfigsList == null) {
      clientsToUpdateConfigsList = new ArrayList<String>();
      clientsToUpdateConfigsList.add("*");
    }

    String clientsToUpdateConfigs = gson.toJson(clientsToUpdateConfigsList);
    hostParams.put(CLIENTS_TO_UPDATE_CONFIGS, clientsToUpdateConfigs);
    execCmd.setHostLevelParams(hostParams);

    Map<String, String> roleParams = new TreeMap<String, String>();

    // !!! consistent with where custom commands put variables
    // !!! after-INSTALL hook checks this such that the stack selection tool won't
    // select-all to a version that is not being upgraded, breaking RU
    if (cluster.isUpgradeSuspended()) {
      roleParams.put(KeyNames.UPGRADE_SUSPENDED, Boolean.TRUE.toString().toLowerCase());
    }

    execCmd.setRoleParams(roleParams);

    execCmd.setAvailableServicesFromServiceInfoMap(ambariMetaInfo.getServices(stackId.getStackName(), stackId.getStackVersion()));


    if ((execCmd != null) && (execCmd.getConfigurationTags().containsKey("cluster-env"))) {
      LOG.debug("AmbariManagementControllerImpl.createHostAction: created ExecutionCommand for host {}, role {}, roleCommand {}, and command ID {}, with cluster-env tags {}",
        execCmd.getHostname(), execCmd.getRole(), execCmd.getRoleCommand(), execCmd.getCommandId(), execCmd.getConfigurationTags().get("cluster-env").get("tag"));
    }
  }

  /**
   * Computes os-dependent packages for service/host. Does not take into
   * account package dependencies for ANY_OS. Instead of this method
   * you should use getPackagesForServiceHost()
   * because it takes into account both os-dependent and os-independent lists
   * of packages for service.
   * @param hostParams may be modified (appended SERVICE_REPO_INFO)
   * @return a list of os-dependent packages for host
   */
  protected ServiceOsSpecific populateServicePackagesInfo(ServiceInfo serviceInfo, Map<String, String> hostParams,
                                                        String osFamily) {
    ServiceOsSpecific hostOs = new ServiceOsSpecific(osFamily);
    List<ServiceOsSpecific> foundOSSpecifics = getOSSpecificsByFamily(serviceInfo.getOsSpecifics(), osFamily);
    if (!foundOSSpecifics.isEmpty()) {
      for (ServiceOsSpecific osSpecific : foundOSSpecifics) {
        hostOs.addPackages(osSpecific.getPackages());
      }
      // Choose repo that is relevant for host
      ServiceOsSpecific.Repo serviceRepo = hostOs.getRepo();
      if (serviceRepo != null) {
        String serviceRepoInfo = gson.toJson(serviceRepo);
        hostParams.put(SERVICE_REPO_INFO, serviceRepoInfo);
      }
    }
    return hostOs;
  }

  @Override
  public List<ServiceOsSpecific.Package> getPackagesForServiceHost(ServiceInfo serviceInfo, Map<String, String> hostParams, String osFamily) {
    // Write down os specific info for the service
    ServiceOsSpecific anyOs = null;
    if (serviceInfo.getOsSpecifics().containsKey(AmbariMetaInfo.ANY_OS)) {
      anyOs = serviceInfo.getOsSpecifics().get(AmbariMetaInfo.ANY_OS);
    }

    ServiceOsSpecific hostOs = populateServicePackagesInfo(serviceInfo, hostParams, osFamily);

    // Build package list that is relevant for host
    List<ServiceOsSpecific.Package> packages =
            new ArrayList<ServiceOsSpecific.Package>();
    if (anyOs != null) {
      packages.addAll(anyOs.getPackages());
    }

    if (hostOs != null) {
      packages.addAll(hostOs.getPackages());
    }

    return packages;
  }

  private List<ServiceOsSpecific> getOSSpecificsByFamily(Map<String, ServiceOsSpecific> osSpecifics, String osFamily) {
    List<ServiceOsSpecific> foundedOSSpecifics = new ArrayList<ServiceOsSpecific>();
    for (Entry<String, ServiceOsSpecific> osSpecific : osSpecifics.entrySet()) {
      if (osSpecific.getKey().contains(osFamily)) {
        foundedOSSpecifics.add(osSpecific.getValue());
      }
    }
    return foundedOSSpecifics;
  }

  private ActionExecutionContext getActionExecutionContext
          (ExecuteActionRequest actionRequest) throws AmbariException {
    RequestOperationLevel operationLevel = actionRequest.getOperationLevel();
    if (actionRequest.isCommand()) {
      ActionExecutionContext actionExecutionContext =
              new ActionExecutionContext(actionRequest.getClusterName(),
              actionRequest.getCommandName(), actionRequest.getResourceFilters(),
              actionRequest.getParameters());
      actionExecutionContext.setOperationLevel(operationLevel);
      return actionExecutionContext;
    } else { // If action

      ActionDefinition actionDef =
              ambariMetaInfo.getActionDefinition(actionRequest.getActionName());

      if (actionDef == null) {
        throw new AmbariException(
                "Action " + actionRequest.getActionName() + " does not exist");
      }

      ActionExecutionContext actionExecutionContext =
              new ActionExecutionContext(actionRequest.getClusterName(),
              actionRequest.getActionName(), actionRequest.getResourceFilters(),
              actionRequest.getParameters(), actionDef.getTargetType(),
              actionDef.getDefaultTimeout(), actionDef.getTargetService(),
              actionDef.getTargetComponent());
      actionExecutionContext.setOperationLevel(operationLevel);
      return actionExecutionContext;
    }
  }

  protected RequestStageContainer doStageCreation(RequestStageContainer requestStages,
      Cluster cluster,
      Map<State, List<Service>> changedServices,
      Map<State, List<ServiceComponent>> changedComps,
      Map<String, Map<State, List<ServiceComponentHost>>> changedScHosts,
      Map<String, String> requestParameters,
      Map<String, String> requestProperties,
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
      LOG.info("Created 0 stages");
      return requestStages;
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
      Map<String, Set<String>> clusterHostInfo = StageUtils.getClusterHostInfo(cluster);

      String clusterHostInfoJson = StageUtils.getGson().toJson(clusterHostInfo);
      String hostParamsJson = StageUtils.getGson().toJson(
          customCommandExecutionHelper.createDefaultHostParams(cluster));

      Stage stage = createNewStage(requestStages.getLastStageId(), cluster,
          requestStages.getId(), requestProperties.get(REQUEST_CONTEXT_PROPERTY),
          clusterHostInfoJson, "{}", hostParamsJson);
      boolean skipFailure = false;
      if (requestProperties.containsKey(Setting.SETTING_NAME_SKIP_FAILURE) && requestProperties.get(Setting.SETTING_NAME_SKIP_FAILURE).equalsIgnoreCase("true")) {
        skipFailure = true;
      }
      stage.setAutoSkipFailureSupported(skipFailure);
      stage.setSkippable(skipFailure);

      Collection<ServiceComponentHost> componentsToEnableKerberos = new ArrayList<>();
      Set<String> hostsToForceKerberosOperations = new HashSet<>();

      /* *******************************************************************************************
       * If Kerberos is enabled, pre-process the changed components to update any configurations and
       * indicate which components may need to have principals or keytab files created.
       *
       * NOTE: Configurations need to be updated before tasks are created to install components
       *       so that any configuration changes are included before the task is queued.
       *
       *       Kerberos-related stages need to be inserted between the INSTALLED and STARTED states
       *       because some services need to set up the host (i,e, create user accounts, etc...)
       *       before Kerberos-related tasks an occur (like distribute keytabs)
       * **************************************************************************************** */
      if(kerberosHelper.isClusterKerberosEnabled(cluster)) {
        Collection<ServiceComponentHost> componentsToConfigureForKerberos = new ArrayList<>();

        for (Map<State, List<ServiceComponentHost>> changedScHostStates : changedScHosts.values()) {

          if (changedScHostStates != null) {
            for (Map.Entry<State, List<ServiceComponentHost>> changedScHostState : changedScHostStates.entrySet()) {
              State newState = changedScHostState.getKey();

              if (newState == State.INSTALLED) {
                List<ServiceComponentHost> scHosts = changedScHostState.getValue();

                if (scHosts != null) {
                  for (ServiceComponentHost scHost : scHosts) {
                    State oldSchState = scHost.getState();

                    // If the state is transitioning from INIT TO INSTALLED and the cluster has Kerberos
                    // enabled, mark this ServiceComponentHost to see if anything needs to be done to
                    // make sure it is properly configured.
                    //
                    // If the component is transitioning from an INSTALL_FAILED to an INSTALLED state
                    // indicates a failure attempt on install followed by a new installation attempt and
                    // will also need consideration for Kerberos-related tasks
                    if ((oldSchState == State.INIT || oldSchState == State.INSTALL_FAILED)) {
                      // Check if the host component already exists, if it exists there is no need to
                      // reset Kerberos-related configs.
                      // Check if it's blueprint install. If it is, then do not configure this service
                      // at this time.
                      if (!hostComponentAlreadyExists(cluster, scHost) && !("INITIAL_INSTALL".equals(requestProperties.get("phase")))) {
                        componentsToConfigureForKerberos.add(scHost);
                      }

                      // Add the ServiceComponentHost to the componentsToEnableKerberos Set to indicate
                      // it may need Kerberos-related operations to be performed on its behalf.
                      // For example, creating principals and keytab files.
                      componentsToEnableKerberos.add(scHost);

                      if (Service.Type.KERBEROS.name().equalsIgnoreCase(scHost.getServiceName()) &&
                          Role.KERBEROS_CLIENT.name().equalsIgnoreCase(scHost.getServiceComponentName())) {
                        // Since the KERBEROS/KERBEROS_CLIENT is about to be moved from the INIT to the
                        // INSTALLED state (and it should be by the time the stages (in this request)
                        // that need to be execute), collect the relevant hostname to make sure the
                        // Kerberos logic doest not skip operations for it.
                        hostsToForceKerberosOperations.add(scHost.getHostName());
                      }
                    }
                  }
                }
              }
            }
          }
        }

        // If there are any components that may need Kerberos-related configuration changes, do it
        // here - before the INSTALL tasks get created so the configuration updates are set and
        // get included in the task details.
        if (!componentsToConfigureForKerberos.isEmpty()) {
          // Build service/component filter to declare what services and components are being added
          // so kerberosHelper.configureServices know which to work on.  Null indicates no filter
          // and all services and components will be (re)configured, however null will not be
          // passed in from here.
          Map<String, Collection<String>> serviceFilter = new HashMap<String, Collection<String>>();

          for (ServiceComponentHost scHost : componentsToConfigureForKerberos) {
            String serviceName = scHost.getServiceName();
            Collection<String> componentFilter = serviceFilter.get(serviceName);

            if (componentFilter == null) {
              componentFilter = new HashSet<String>();
              serviceFilter.put(serviceName, componentFilter);
            }

            componentFilter.add(scHost.getServiceComponentName());
          }

          try {
            kerberosHelper.configureServices(cluster, serviceFilter);
          } catch (KerberosInvalidConfigurationException e) {
            throw new AmbariException(e.getMessage(), e);
          }
        }
      }

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

                  if (scHost.isClientComponent() && oldSchState == State.INSTALLED) {
                    // Client reinstalls are executed to reattach changed configs on service.
                    // Do not transition a client component to INSTALLING state if it was installed.
                    // Prevents INSTALL_FAILED state if a command gets aborted.
                    event = new ServiceComponentHostOpInProgressEvent(
                        scHost.getServiceComponentName(), scHost.getHostName(),
                        nowTimestamp);
                  } else {
                    event = new ServiceComponentHostInstallEvent(
                        scHost.getServiceComponentName(), scHost.getHostName(),
                        nowTimestamp,
                        scHost.getDesiredStackVersion().getStackId());
                  }
                } else if (oldSchState == State.STARTED
                      // TODO: oldSchState == State.INSTALLED is always false, looks like a bug
                      //|| oldSchState == State.INSTALLED
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
                ComponentInfo compInfo = ambariMetaInfo.getComponent(
                    stackId.getStackName(), stackId.getStackVersion(), scHost.getServiceName(),
                    scHost.getServiceComponentName());

                if (oldSchState == State.INSTALLED ||
                    oldSchState == State.STARTING ||
                    //todo: after separating install and start, the install stage is no longer in request stage container
                    //todo: so projected state will not equal INSTALLED which causes an exception for invalid state transition
                    //todo: so for now disabling this check
                    //todo: this change breaks test AmbariManagementControllerTest.testServiceComponentHostUpdateRecursive()
                    true) {
//                    requestStages.getProjectedState(scHost.getHostName(),
//                        scHost.getServiceComponentName()) == State.INSTALLED) {
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

            // any targeted information
            String keyName = scHost.getServiceComponentName().toLowerCase();
            if (requestProperties.containsKey(keyName)) {
              // in the case where the command is targeted, but the states
              // of the old and new are the same, the targeted component
              // may still need to get the command.  This is true for Flume.
              if (oldSchState == newState) {
                switch (oldSchState) {
                  case INSTALLED:
                    roleCommand = RoleCommand.STOP;
                    event = new ServiceComponentHostStopEvent(
                        scHost.getServiceComponentName(), scHost.getHostName(),
                        nowTimestamp);
                    break;
                  case STARTED:
                    roleCommand = RoleCommand.START;
                    event = new ServiceComponentHostStartEvent(
                        scHost.getServiceComponentName(), scHost.getHostName(),
                        nowTimestamp);
                    break;
                  default:
                    break;
                }
              }

              if (null == requestParameters) {
                requestParameters = new HashMap<String, String>();
              }
              requestParameters.put(keyName, requestProperties.get(keyName));
            }

            if (requestProperties.containsKey(CLUSTER_PHASE_PROPERTY)) {
              if (null == requestParameters) {
                requestParameters = new HashMap<String, String>();
              }
              requestParameters.put(CLUSTER_PHASE_PROPERTY, requestProperties.get(CLUSTER_PHASE_PROPERTY));
            }

            Map<String, Map<String, String>> configurations = new TreeMap<String, Map<String, String>>();
            Map<String, Map<String, Map<String, String>>>
                configurationAttributes =
                new TreeMap<String, Map<String, Map<String, String>>>();
            Host host = clusters.getHost(scHost.getHostName());

            Map<String, Map<String, String>> configTags =
                findConfigurationTagsWithOverrides(cluster, host.getHostName());

            // Skip INSTALL task in case SysPrepped hosts and in case of server components. In case of server component
            // START task should run configuration script.
            if (newState == State.INSTALLED && skipInstallTaskForComponent(requestProperties, cluster, scHost)) {
              LOG.info("Skipping create of INSTALL task for {} on {}.", scHost.getServiceComponentName(), scHost.getHostName());
              scHost.setState(State.INSTALLED);
            } else {
              createHostAction(cluster, stage, scHost, configurations, configurationAttributes, configTags,
                roleCommand, requestParameters, event, skipFailure);
            }

          }
        }
      }

      for (String serviceName : smokeTestServices) { // Creates smoke test commands
        Service s = cluster.getService(serviceName);
        // find service component host
        ServiceComponent component = getClientComponentForRunningAction(cluster, s);
        String componentName = component != null ? component.getName() : null;
        String clientHost = getClientHostForRunningAction(cluster, s, component);
        String smokeTestRole = actionMetadata.getServiceCheckAction(serviceName);

        if (clientHost == null || smokeTestRole == null) {
          LOG.info("Nothing to do for service check as could not find role or"
              + " or host to run check on"
              + ", clusterName=" + cluster.getClusterName()
              + ", serviceName=" + serviceName
              + ", clientHost=" + clientHost
              + ", serviceCheckRole=" + smokeTestRole);
          continue;
        }

        customCommandExecutionHelper.addServiceCheckAction(stage, clientHost, smokeTestRole,
            nowTimestamp, serviceName, componentName, null, false, false);
      }

      RoleCommandOrder rco = getRoleCommandOrder(cluster);
      RoleGraph rg = roleGraphFactory.createNew(rco);


      if (CommandExecutionType.DEPENDENCY_ORDERED == configs.getStageExecutionType() && "INITIAL_START".equals
        (requestProperties.get("phase"))) {
        LOG.info("Set DEPENDENCY_ORDERED CommandExecutionType on stage: {}", stage.getRequestContext());
        rg.setCommandExecutionType(CommandExecutionType.DEPENDENCY_ORDERED);
      }
      rg.build(stage);
      requestStages.addStages(rg.getStages());

      if (!componentsToEnableKerberos.isEmpty()) {
        Map<String, Collection<String>> serviceFilter = new HashMap<String, Collection<String>>();
        Set<String> hostFilter = new HashSet<String>();

        for (ServiceComponentHost scHost : componentsToEnableKerberos) {
          String serviceName = scHost.getServiceName();
          Collection<String> componentFilter = serviceFilter.get(serviceName);

          if (componentFilter == null) {
            componentFilter = new HashSet<String>();
            serviceFilter.put(serviceName, componentFilter);
          }

          componentFilter.add(scHost.getServiceComponentName());
          hostFilter.add(scHost.getHostName());
        }

        try {
          kerberosHelper.ensureIdentities(cluster, serviceFilter, hostFilter, null, hostsToForceKerberosOperations, requestStages,
              kerberosHelper.getManageIdentitiesDirective(requestProperties));
        } catch (KerberosOperationException e) {
          throw new IllegalArgumentException(e.getMessage(), e);
        }
      }

      List<Stage> stages = requestStages.getStages();
      LOG.debug("Created {} stages", ((stages != null) ? stages.size() : 0));

    } else {
      LOG.debug("Created 0 stages");
    }

    return requestStages;
  }

  private boolean hostComponentAlreadyExists(Cluster cluster, ServiceComponentHost sch) throws AmbariException {
    Service service = cluster.getService(sch.getServiceName());
    if (service != null) {
      ServiceComponent serviceComponent = service.getServiceComponent(sch.getServiceComponentName());
      if (serviceComponent != null) {
        Map<String, ServiceComponentHost> serviceComponentHostMap = serviceComponent.getServiceComponentHosts();
        for (ServiceComponentHost serviceComponentHost : serviceComponentHostMap.values()) {
          if (serviceComponentHost.getState() == State.INSTALLED || serviceComponentHost.getState() == State.STARTED) {
            return true;
          }
        }
      }
    }
    return false;
  }


  private boolean skipInstallTaskForComponent(Map<String, String> requestProperties, Cluster cluster,
                                              ServiceComponentHost sch) throws AmbariException {
    boolean isClientComponent = false;
    Service service = cluster.getService(sch.getServiceName());
    if (service != null) {
      ServiceComponent serviceComponent = service.getServiceComponent(sch.getServiceComponentName());
      if (serviceComponent != null) {
        isClientComponent = serviceComponent.isClientComponent();
      }
    }
    // Skip INSTALL for service components if START_ONLY is set for component, or if START_ONLY is set on cluster
    // level and no other provsion action is specified for component
    if (requestProperties.get(SKIP_INSTALL_FOR_COMPONENTS) != null &&
      (requestProperties.get(SKIP_INSTALL_FOR_COMPONENTS).contains(sch.getServiceComponentName()) ||
        (requestProperties.get(SKIP_INSTALL_FOR_COMPONENTS).equals("ALL") && !requestProperties.get
          (DONT_SKIP_INSTALL_FOR_COMPONENTS).contains(sch
          .getServiceComponentName()))) &&
      "INITIAL_INSTALL".equals(requestProperties.get("phase")) && !isClientComponent) {
      return true;
    }
    return false;

  }

  @Override
  public ExecutionCommand getExecutionCommand(Cluster cluster,
                                              ServiceComponentHost scHost,
                                              RoleCommand roleCommand) throws AmbariException {
    Map<String, Set<String>> clusterHostInfo = StageUtils.getClusterHostInfo(cluster);
    String clusterHostInfoJson = StageUtils.getGson().toJson(clusterHostInfo);
    Map<String, String> hostParamsCmd = customCommandExecutionHelper.createDefaultHostParams(cluster);
    Stage stage = createNewStage(0, cluster,
                                 1, "",
                                 clusterHostInfoJson, "{}", "");


    Map<String, Map<String, String>> configTags = configHelper.getEffectiveDesiredTags(cluster, scHost.getHostName());
    Map<String, Map<String, String>> configurations = configHelper.getEffectiveConfigProperties(cluster, configTags);

    Map<String, Map<String, Map<String, String>>>
        configurationAttributes =
        new TreeMap<String, Map<String, Map<String, String>>>();

    createHostAction(cluster, stage, scHost, configurations, configurationAttributes, configTags,
                     roleCommand, null, null, false);
    ExecutionCommand ec = stage.getExecutionCommands().get(scHost.getHostName()).get(0).getExecutionCommand();

    // createHostAction does not take a hostLevelParams but creates one
    hostParamsCmd.putAll(ec.getHostLevelParams());
    ec.getHostLevelParams().putAll(hostParamsCmd);

    ec.setClusterHostInfo(
        StageUtils.getClusterHostInfo(cluster));

    if (null != cluster) {
      // Generate localComponents
      for (ServiceComponentHost sch : cluster.getServiceComponentHosts(scHost.getHostName())) {
        ec.getLocalComponents().add(sch.getServiceComponentName());
      }
    }

    ConfigHelper.processHiddenAttribute(ec.getConfigurations(), ec.getConfigurationAttributes(), ec.getRole(), false);

    // Add attributes
    Map<String, Map<String, Map<String, String>>> configAttributes =
        configHelper.getEffectiveConfigAttributes(cluster,
          ec.getConfigurationTags());

    for (Map.Entry<String, Map<String, Map<String, String>>> attributesOccurrence : configAttributes.entrySet()) {
      String type = attributesOccurrence.getKey();
      Map<String, Map<String, String>> attributes = attributesOccurrence.getValue();

      if (ec.getConfigurationAttributes() != null) {
        if (!ec.getConfigurationAttributes().containsKey(type)) {
          ec.getConfigurationAttributes().put(type, new TreeMap<String, Map<String, String>>());
        }
        configHelper.cloneAttributesMap(attributes, ec.getConfigurationAttributes().get(type));
      }
    }

    return ec;
  }

  @Override
  public Set<StackConfigurationDependencyResponse> getStackConfigurationDependencies(
          Set<StackConfigurationDependencyRequest> requests) throws AmbariException {
    Set<StackConfigurationDependencyResponse> response
            = new HashSet<StackConfigurationDependencyResponse>();
    if (requests != null) {
      for (StackConfigurationDependencyRequest request : requests) {

        String stackName = request.getStackName();
        String stackVersion = request.getStackVersion();
        String serviceName = request.getServiceName();
        String propertyName = request.getPropertyName();

        Set<StackConfigurationDependencyResponse> stackConfigurations
                = getStackConfigurationDependencies(request);

        for (StackConfigurationDependencyResponse dependencyResponse : stackConfigurations) {
          dependencyResponse.setStackName(stackName);
          dependencyResponse.setStackVersion(stackVersion);
          dependencyResponse.setServiceName(serviceName);
          dependencyResponse.setPropertyName(propertyName);
        }
        response.addAll(stackConfigurations);
      }
    }
    return response;
  }

  private Set<StackConfigurationDependencyResponse> getStackConfigurationDependencies(StackConfigurationDependencyRequest request) throws AmbariException {
    Set<StackConfigurationDependencyResponse> response =
      new HashSet<StackConfigurationDependencyResponse>();

    String stackName = request.getStackName();
    String stackVersion = request.getStackVersion();
    String serviceName = request.getServiceName();
    String propertyName = request.getPropertyName();
    String dependencyName = request.getDependencyName();

    Set<PropertyInfo> properties = ambariMetaInfo.getPropertiesByName(stackName, stackVersion, serviceName, propertyName);

    for (PropertyInfo property: properties) {
      for (PropertyDependencyInfo dependency: property.getDependedByProperties()) {
        if (dependencyName == null || dependency.getName().equals(dependencyName)) {
          response.add(dependency.convertToResponse());
        }
      }
    }

    return response;  }

  @Transactional
  void updateServiceStates(
      Cluster cluster,
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
          changedComps.entrySet()) {
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

    requestStages = doStageCreation(requestStages, cluster, changedServices, changedComponents,
        changedHosts, requestParameters, requestProperties,
        runSmokeTest, reconfigureClients);

    updateServiceStates(cluster, changedServices, changedComponents, changedHosts, ignoredHosts);

    return requestStages;
  }

  //todo: for now made this public since is is still used by createHostComponents
  //todo: delete after all host component logic is in HostComponentResourceProvider
  public void validateServiceComponentHostRequest(ServiceComponentHostRequest request) {
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

  private void checkIfHostComponentsInDeleteFriendlyState(ServiceComponentHostRequest request, Cluster cluster) throws AmbariException {
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
  }

  @Override
  public String findServiceName(Cluster cluster, String componentName) throws AmbariException {
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

  /**
   * Updates the users specified.
   *
   * @param requests the users to modify
   *
   * @throws AmbariException if the resources cannot be updated
   * @throws IllegalArgumentException if the authenticated user is not authorized to update all of
   * the requested properties
   */
  @Override
  public synchronized void updateUsers(Set<UserRequest> requests) throws AmbariException, AuthorizationException {
    boolean isUserAdministrator = AuthorizationHelper.isAuthorized(ResourceType.AMBARI, null,
        RoleAuthorization.AMBARI_MANAGE_USERS);
    String authenticatedUsername = AuthorizationHelper.getAuthenticatedName();

    for (UserRequest request : requests) {
      String requestedUsername = request.getUsername();

      // An administrator can modify any user, else a user can only modify themself.
      if (!isUserAdministrator && (!authenticatedUsername.equalsIgnoreCase(requestedUsername))) {
        throw new AuthorizationException();
      }

      User u = users.getAnyUser(requestedUsername);
      if (null == u) {
        continue;
      }

      if (null != request.isActive()) {
        // If this value is being set, make sure the authenticated user is an administrator before
        // allowing to change it. Only administrators should be able to change a user's active state
        if (!isUserAdministrator) {
          throw new AuthorizationException("The authenticated user is not authorized to update the requested resource property");
        }
        users.setUserActive(u.getUserName(), request.isActive());
      }

      if (null != request.isAdmin()) {
        // If this value is being set, make sure the authenticated user is an administrator before
        // allowing to change it. Only administrators should be able to change a user's administrative
        // privileges
        if (!isUserAdministrator) {
          throw new AuthorizationException("The authenticated user is not authorized to update the requested resource property");
        }

        if (request.isAdmin()) {
          users.grantAdminPrivilege(u.getUserId());
        } else {
          users.revokeAdminPrivilege(u.getUserId());
        }
      }

      if (null != request.getOldPassword() && null != request.getPassword()) {
        users.modifyPassword(u.getUserName(), request.getOldPassword(),
            request.getPassword());
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
  public DeleteStatusMetaData deleteHostComponents(
      Set<ServiceComponentHostRequest> requests) throws AmbariException, AuthorizationException {

    Set<ServiceComponentHostRequest> expanded = new HashSet<>();

    // if any request are for the whole host, they need to be expanded
    for (ServiceComponentHostRequest request : requests) {
      if (null == request.getComponentName()) {
        if (null == request.getClusterName() || request.getClusterName().isEmpty() ||
            null == request.getHostname() || request.getHostname().isEmpty()) {
          throw new IllegalArgumentException("Cluster name and hostname must be specified.");
        }
        Cluster cluster = clusters.getCluster(request.getClusterName());

        if(!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, cluster.getResourceId(),
            EnumSet.of(RoleAuthorization.SERVICE_ADD_DELETE_SERVICES,RoleAuthorization.HOST_ADD_DELETE_COMPONENTS))) {
          throw new AuthorizationException("The authenticated user is not authorized to delete service components from hosts");
        }

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

    Map<ServiceComponent, Set<ServiceComponentHost>> safeToRemoveSCHs = new HashMap<>();
    DeleteStatusMetaData deleteStatusMetaData = new DeleteStatusMetaData();

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

      setRestartRequiredServices(service, request.getComponentName());
      try {
        checkIfHostComponentsInDeleteFriendlyState(request, cluster);
        if (!safeToRemoveSCHs.containsKey(component)) {
          safeToRemoveSCHs.put(component, new HashSet<ServiceComponentHost>());
        }
        safeToRemoveSCHs.get(component).add(componentHost);
      } catch (Exception ex) {
        deleteStatusMetaData.addException(request.getHostname() + "/" + request.getComponentName(), ex);
      }
    }

    for (Entry<ServiceComponent, Set<ServiceComponentHost>> entry : safeToRemoveSCHs.entrySet()) {
      for (ServiceComponentHost componentHost : entry.getValue()) {
        try {
          deleteHostComponent(entry.getKey(), componentHost);
          deleteStatusMetaData.addDeletedKey(componentHost.getHostName() + "/" + componentHost.getServiceComponentName());

        } catch (Exception ex) {
          deleteStatusMetaData.addException(componentHost.getHostName() + "/" + componentHost.getServiceComponentName(), ex);
        }
      }
    }

    //Do not break behavior for existing clients where delete request contains only 1 host component.
    //Response for these requests will have empty body with appropriate error code.
    if (deleteStatusMetaData.getDeletedKeys().size() + deleteStatusMetaData.getExceptionForKeys().size() == 1) {
      if (deleteStatusMetaData.getDeletedKeys().size() == 1) {
        return null;
      }
      Exception ex =  deleteStatusMetaData.getExceptionForKeys().values().iterator().next();
      if (ex instanceof AmbariException) {
        throw (AmbariException)ex;
      } else {
        throw new AmbariException(ex.getMessage(), ex);
      }
    }

    // set restartRequired flag for  monitoring services
    if (!safeToRemoveSCHs.isEmpty()) {
      setMonitoringServicesRestartRequired(requests);
    }
    return deleteStatusMetaData;
  }

  private void deleteHostComponent(ServiceComponent serviceComponent, ServiceComponentHost componentHost) throws AmbariException {
    String included_hostname = componentHost.getHostName();
    String serviceName = serviceComponent.getServiceName();
    String master_component_name = null;
    String slave_component_name = componentHost.getServiceComponentName();
    HostComponentAdminState desiredAdminState = componentHost.getComponentAdminState();
    State slaveState = componentHost.getState();
    //Delete hostcomponents
    serviceComponent.deleteServiceComponentHosts(componentHost.getHostName());
    // If deleted hostcomponents support decomission and were decommited and stopped
    if (AmbariCustomCommandExecutionHelper.masterToSlaveMappingForDecom.containsValue(slave_component_name)
            && desiredAdminState.equals(HostComponentAdminState.DECOMMISSIONED)
            && slaveState.equals(State.INSTALLED)) {

      for (Entry<String, String> entrySet : AmbariCustomCommandExecutionHelper.masterToSlaveMappingForDecom.entrySet()) {
        if (entrySet.getValue().equals(slave_component_name)) {
          master_component_name = entrySet.getKey();
        }
      }
      //Clear exclud file or draining list except HBASE
      if (!serviceName.equals(Service.Type.HBASE.toString())) {
        HashMap<String, String> requestProperties = new HashMap<String, String>();
        requestProperties.put("context", "Remove host " +
                included_hostname + " from exclude file");
        requestProperties.put("exclusive", "true");
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("included_hosts", included_hostname);
        params.put("slave_type", slave_component_name);
        params.put(AmbariCustomCommandExecutionHelper.UPDATE_EXCLUDE_FILE_ONLY, "true");

        //Create filter for RECOMISSION command
        RequestResourceFilter resourceFilter
                = new RequestResourceFilter(serviceName, master_component_name, null);
        //Create request for RECOMISSION command
        ExecuteActionRequest actionRequest = new ExecuteActionRequest(
                serviceComponent.getClusterName(), AmbariCustomCommandExecutionHelper.DECOMMISSION_COMMAND_NAME, null,
                Collections.singletonList(resourceFilter), null, params, true);
        //Send request
        createAction(actionRequest, requestProperties);
      }

      //Mark master component as needed to restart for remove host info from components UI
      Cluster cluster = clusters.getCluster(serviceComponent.getClusterName());
      Service service = cluster.getService(serviceName);
      ServiceComponent sc = service.getServiceComponent(master_component_name);

      if (sc != null && sc.isMasterComponent()) {
        for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
          sch.setRestartRequired(true);
        }
      }
    }
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
      if (null != u) {
        users.removeUser(u);
      }
    }
  }

  @Override
  public void deleteGroups(Set<GroupRequest> requests) throws AmbariException {
    for (GroupRequest request: requests) {
      LOG.debug("Received a delete group request, groupname=" + request.getGroupName());
      final Group group = users.getGroup(request.getGroupName());
      if (group != null) {
        users.removeGroup(group);
      }
    }
  }

  @Override
  public void deleteMembers(java.util.Set<MemberRequest> requests) throws AmbariException {
    for (MemberRequest request : requests) {
      LOG.debug("Received a delete member request, " + request);
      users.removeMemberFromGroup(request.getGroupName(), request.getUserName());
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
  public Set<ClusterResponse> getClusters(Set<ClusterRequest> requests) throws AmbariException, AuthorizationException {
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
        if (throwException) {
          throw e;
        }
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
  public Set<ServiceConfigVersionResponse> getServiceConfigVersions(Set<ServiceConfigVersionRequest> requests)
      throws AmbariException {
    Set<ServiceConfigVersionResponse> responses = new LinkedHashSet<ServiceConfigVersionResponse>();

    for (ServiceConfigVersionRequest request : requests) {
      responses.addAll(getServiceConfigVersions(request));
    }

    return responses;
  }

  private Set<ServiceConfigVersionResponse> getServiceConfigVersions(ServiceConfigVersionRequest request)
      throws AmbariException {
    if (request.getClusterName() == null) {
      throw new IllegalArgumentException("Invalid arguments, cluster name"
          + " should not be null");
    }

    Cluster cluster = clusters.getCluster(request.getClusterName());

    Set<ServiceConfigVersionResponse> result = new LinkedHashSet<ServiceConfigVersionResponse>();
    String serviceName = request.getServiceName();
    List<ServiceConfigVersionResponse> serviceConfigVersionResponses =  new ArrayList<ServiceConfigVersionResponse>();

    if (Boolean.TRUE.equals(request.getIsCurrent()) && serviceName != null) {
      serviceConfigVersionResponses.addAll(cluster.getActiveServiceConfigVersionResponse(serviceName));
    } else {
      serviceConfigVersionResponses.addAll(cluster.getServiceConfigVersions());
    }

    for (ServiceConfigVersionResponse response : serviceConfigVersionResponses) {
      if (serviceName != null && !StringUtils.equals(serviceName, response.getServiceName())) {
        continue;
      }
      if (request.getVersion() != null && NumberUtils.compare(request.getVersion(), response.getVersion()) != 0) {
        continue;
      }
      if (request.getUserName() != null && !StringUtils.equals(request.getUserName(), response.getUserName())) {
        continue;
      }
      result.add(response);
    }

    return result;
  }

  @Override
  public Set<UserResponse> getUsers(Set<UserRequest> requests)
      throws AmbariException, AuthorizationException {

    Set<UserResponse> responses = new HashSet<UserResponse>();

    for (UserRequest r : requests) {

      if (LOG.isDebugEnabled()) {
        LOG.debug("Received a getUsers request"
            + ", userRequest=" + r.toString());
      }

      String requestedUsername = r.getUsername();
      String authenticatedUsername = AuthorizationHelper.getAuthenticatedName();

      // A user resource may be retrieved by an administrator or the same user.
      if(!AuthorizationHelper.isAuthorized(ResourceType.AMBARI, null, RoleAuthorization.AMBARI_MANAGE_USERS)) {
        if (null == requestedUsername) {
          // Since the authenticated user is not the administrator, force only that user's resource
          // to be returned
          requestedUsername = authenticatedUsername;
        } else if (!requestedUsername.equalsIgnoreCase(authenticatedUsername)) {
          // Since the authenticated user is not the administrator and is asking for a different user,
          // throw an AuthorizationException
          throw new AuthorizationException();
        }
      }

      // get them all
      if (null == requestedUsername) {
        for (User u : users.getAllUsers()) {
          UserResponse resp = new UserResponse(u.getUserName(), u.getUserType(), u.isLdapUser(), u.isActive(), u
              .isAdmin());
          resp.setGroups(new HashSet<String>(u.getGroups()));
          responses.add(resp);
        }
      } else {

        User u = users.getAnyUser(requestedUsername);
        if (null == u) {
          if (requests.size() == 1) {
            // only throw exceptin if there is a single request
            // if there are multiple requests, this indicates an OR predicate
            throw new ObjectNotFoundException("Cannot find user '"
                + requestedUsername + "'");
          }
        } else {
          UserResponse resp = new UserResponse(u.getUserName(), u.getUserType(), u.isLdapUser(), u.isActive(), u
              .isAdmin());
          resp.setGroups(new HashSet<String>(u.getGroups()));
          responses.add(resp);
        }
      }
    }

    return responses;
  }

  @Override
  public Set<GroupResponse> getGroups(Set<GroupRequest> requests)
      throws AmbariException {
    final Set<GroupResponse> responses = new HashSet<GroupResponse>();
    for (GroupRequest request: requests) {
      LOG.debug("Received a getGroups request, groupRequest=" + request.toString());
      // get them all
      if (null == request.getGroupName()) {
        for (Group group: users.getAllGroups()) {
          final GroupResponse response = new GroupResponse(group.getGroupName(), group.isLdapGroup(), group.getGroupType());
          responses.add(response);
        }
      } else {
        final Group group = users.getGroup(request.getGroupName());
        if (null == group) {
          if (requests.size() == 1) {
            // only throw exception if there is a single request
            // if there are multiple requests, this indicates an OR predicate
            throw new ObjectNotFoundException("Cannot find group '"
                + request.getGroupName() + "'");
          }
        } else {
          final GroupResponse response = new GroupResponse(group.getGroupName(), group.isLdapGroup(), group.getGroupType());
          responses.add(response);
        }
      }
    }
    return responses;
  }

  @Override
  public void updateGroups(Set<GroupRequest> requests) throws AmbariException {
    // currently no group updates are supported
  }

  protected String getClientHostForRunningAction(Cluster cluster, Service service, ServiceComponent serviceComponent)
      throws AmbariException {
    if (serviceComponent != null && !serviceComponent.getServiceComponentHosts().isEmpty()) {
      Set<String> candidateHosts = serviceComponent.getServiceComponentHosts().keySet();
      filterHostsForAction(candidateHosts, service, cluster, Resource.Type.Cluster);
      return getHealthyHost(candidateHosts);
    }
    return null;
  }

  protected ServiceComponent getClientComponentForRunningAction(Cluster cluster,
      Service service) throws AmbariException {
    /*
     * We assume Cluster level here. That means that we never run service
     * checks on clients/hosts that are in maintenance state.
     * That also means that we can not run service check if the only host
     * that has client component is in maintenance state
     */

    StackId stackId = service.getDesiredStackVersion();
    ComponentInfo compInfo =
        ambariMetaInfo.getService(stackId.getStackName(),
            stackId.getStackVersion(), service.getName()).getClientComponent();
    if (compInfo != null) {
      try {
        ServiceComponent serviceComponent =
            service.getServiceComponent(compInfo.getName());
        if (!serviceComponent.getServiceComponentHosts().isEmpty()) {
          return serviceComponent;
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
    if (!components.isEmpty()) {
      for (ServiceComponent serviceComponent : components.values()) {
        if (!serviceComponent.getServiceComponentHosts().isEmpty()) {
          return serviceComponent;
        }
      }
    }
    return null;
  }

  /**
   * Utility method that filters out hosts from set based on their maintenance
   * state status.
   */
  protected void filterHostsForAction(Set<String> candidateHosts, Service service,
                                    final Cluster cluster,
                                    final Resource.Type level)
                                    throws AmbariException {
    Set<String> ignoredHosts = maintenanceStateHelper.filterHostsInMaintenanceState(
      candidateHosts, new MaintenanceStateHelper.HostPredicate() {
        @Override
        public boolean shouldHostBeRemoved(final String hostname)
          throws AmbariException {
          Host host = clusters.getHost(hostname);
          return !maintenanceStateHelper.isOperationAllowed(
            host, cluster.getClusterId(), level);
        }
      }
    );
    LOG.debug("Ignoring hosts when selecting available hosts for action" +
            " due to maintenance state." +
            "Ignored hosts =" + ignoredHosts + ", cluster="
            + cluster.getClusterName() + ", service=" + service.getName());
  }

  /**
   * Filters hosts to only select healthy ones that are heartbeating.
   * <p/>
   * The host's {@link HostState} is used to determine if a host is healthy.
   *
   * @return a List of healthy hosts, or an empty List if none exist.
   * @throws AmbariException
   * @see {@link HostState#HEALTHY}
   */
  @Override
  public List<String> selectHealthyHosts(Set<String> hostList) throws AmbariException {
    List<String> healthyHosts = new ArrayList<>();

    for (String candidateHostName : hostList) {
      Host candidateHost = clusters.getHost(candidateHostName);
      if (candidateHost.getState() == HostState.HEALTHY) {
        healthyHosts.add(candidateHostName);
      }
    }

    return healthyHosts;
  }

  /**
   * Chooses a healthy host from the list of candidate hosts randomly. If there
   * are no healthy hosts, then this method will return {@code null}.
   * <p/>
   * The host's {@link HostState} is used to determine if a host is healthy.
   *
   * @return a random healthy host, or {@code null}.
   * @throws AmbariException
   * @see {@link HostState#HEALTHY}
   */
  @Override
  public String getHealthyHost(Set<String> hostList) throws AmbariException {
    List<String> healthyHosts = selectHealthyHosts(hostList);

    if (!healthyHosts.isEmpty()) {
      Collections.shuffle(healthyHosts);
      return healthyHosts.get(0);
    }

    return null;
  }

  @Override
  public RequestStatusResponse createAction(ExecuteActionRequest actionRequest,
      Map<String, String> requestProperties)
      throws AmbariException {
    String clusterName = actionRequest.getClusterName();

    String requestContext = "";

    if (requestProperties != null) {
      requestContext = requestProperties.get(REQUEST_CONTEXT_PROPERTY);
      if (requestContext == null) {
        // guice needs a non-null value as there is no way to mark this parameter @Nullable
        requestContext = "";
      }
    }

    Cluster cluster = null;
    if (null != clusterName) {
      cluster = clusters.getCluster(clusterName);

      LOG.info("Received action execution request"
        + ", clusterName=" + actionRequest.getClusterName()
        + ", request=" + actionRequest.toString());
    }

    ActionExecutionContext actionExecContext = getActionExecutionContext(actionRequest);
    if (actionRequest.isCommand()) {
      customCommandExecutionHelper.validateAction(actionRequest);
    } else {
      actionExecutionHelper.validateAction(actionRequest);
    }
    // TODO Alejandro, Called First. insert params.version. Called during Rebalance HDFS, ZOOKEEPER Restart, Zookeeper Service Check.
    long requestId = actionManager.getNextRequestId();
    RequestStageContainer requestStageContainer = new RequestStageContainer(
        requestId,
        null,
        requestFactory,
        actionManager,
        actionRequest);

    StackId stackId = null;
    if (null != cluster) {
      stackId = cluster.getDesiredStackVersion();
    }
    ExecuteCommandJson jsons = customCommandExecutionHelper.getCommandJson(actionExecContext, cluster, stackId);
    String commandParamsForStage = jsons.getCommandParamsForStage();

    Map<String, String> commandParamsStage = gson.fromJson(commandParamsForStage, new TypeToken<Map<String, String>>()
      {}.getType());
    // Ensure that the specified requestContext (if any) is set as the request context
    if (!requestContext.isEmpty()) {
      requestStageContainer.setRequestContext(requestContext);
    }

    // replace password references in requestProperties
    SecretReference.replaceReferencesWithPasswords(commandParamsStage, cluster);

    // If the request is to perform the Kerberos service check, set up the stages to
    // ensure that the (cluster-level) smoke user principal and keytab is available on all hosts
    boolean kerberosServiceCheck = Role.KERBEROS_SERVICE_CHECK.name().equals(actionRequest.getCommandName());
    if (kerberosServiceCheck) {
      // Parse the command parameters into a map so that additional values may be added to it

      try {
        requestStageContainer = kerberosHelper.createTestIdentity(cluster, commandParamsStage, requestStageContainer);
      } catch (KerberosOperationException e) {
        throw new IllegalArgumentException(e.getMessage(), e);
      }
    }

    commandParamsForStage = gson.toJson(commandParamsStage);

    Stage stage = createNewStage(requestStageContainer.getLastStageId(), cluster, requestId, requestContext,
        jsons.getClusterHostInfo(), commandParamsForStage, jsons.getHostParamsForStage());

    if (actionRequest.isCommand()) {
      customCommandExecutionHelper.addExecutionCommandsToStage(actionExecContext, stage,
          requestProperties);
    } else {
      actionExecutionHelper.addExecutionCommandsToStage(actionExecContext, stage, requestProperties);
    }

    RoleGraph rg;
    if (null != cluster) {
      RoleCommandOrder rco = getRoleCommandOrder(cluster);
      rg = roleGraphFactory.createNew(rco);
    } else {
      rg = roleGraphFactory.createNew();
    }

    rg.build(stage);
    List<Stage> stages = rg.getStages();

    if (stages != null && !stages.isEmpty()) {
      requestStageContainer.addStages(stages);
    }

    // If the request is to perform the Kerberos service check, delete the test-specific principal
    // and keytab that was created for this service check
    if (kerberosServiceCheck) {
      // Parse the command parameters into a map so that existing values may be accessed and
      // additional values may be added to it.
      commandParamsStage = gson.fromJson(commandParamsForStage,
          new TypeToken<Map<String, String>>() {
          }.getType());

      try {
        requestStageContainer = kerberosHelper.deleteTestIdentity(cluster, commandParamsStage, requestStageContainer);
      } catch (KerberosOperationException e) {
        throw new IllegalArgumentException(e.getMessage(), e);
      }
    }

    requestStageContainer.persist();
    return requestStageContainer.getRequestStatusResponse();
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
      // this will throw an exception if the stack doesn't exist
      ambariMetaInfo.getStacks(stackName);
      response = Collections.singleton(new StackResponse(stackName));
    } else {
      Collection<StackInfo> supportedStacks = ambariMetaInfo.getStacks();
      response = new HashSet<StackResponse>();
      for (StackInfo stack: supportedStacks) {
        response.add(new StackResponse(stack.getName()));
      }
    }
    return response;
  }

  @Override
  public synchronized RequestStatusResponse updateStacks() throws AmbariException {

    try {
      ambariMetaInfo.init();
    } catch (AmbariException e) {
      throw e;
    } catch (Exception e) {
      throw new AmbariException(
          "Ambari Meta Information can't be read from the stack root directory");
    }

    return null;
  }

  @Override
  public Set<ExtensionResponse> getExtensions(Set<ExtensionRequest> requests)
      throws AmbariException {
    Set<ExtensionResponse> response = new HashSet<ExtensionResponse>();
    for (ExtensionRequest request : requests) {
      try {
        response.addAll(getExtensions(request));
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


  private Set<ExtensionResponse> getExtensions(ExtensionRequest request)
      throws AmbariException {
    Set<ExtensionResponse> response;

    String extensionName = request.getExtensionName();

    if (extensionName != null) {
      // this will throw an exception if the extension doesn't exist
      ambariMetaInfo.getExtensions(extensionName);
      response = Collections.singleton(new ExtensionResponse(extensionName));
    } else {
      Collection<ExtensionInfo> supportedExtensions = ambariMetaInfo.getExtensions();
      response = new HashSet<ExtensionResponse>();
      for (ExtensionInfo extension: supportedExtensions) {
        response.add(new ExtensionResponse(extension.getName()));
      }
    }
    return response;
  }

  @Override
  public Set<ExtensionVersionResponse> getExtensionVersions(
      Set<ExtensionVersionRequest> requests) throws AmbariException {
    Set<ExtensionVersionResponse> response = new HashSet<ExtensionVersionResponse>();
    for (ExtensionVersionRequest request : requests) {
      String extensionName = request.getExtensionName();
      try {
        Set<ExtensionVersionResponse> stackVersions = getExtensionVersions(request);
        for (ExtensionVersionResponse stackVersionResponse : stackVersions) {
          stackVersionResponse.setExtensionName(extensionName);
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

  private Set<ExtensionVersionResponse> getExtensionVersions(ExtensionVersionRequest request) throws AmbariException {
    Set<ExtensionVersionResponse> response;

    String extensionName = request.getExtensionName();
    String extensionVersion = request.getExtensionVersion();

    if (extensionVersion != null) {
      ExtensionInfo extensionInfo = ambariMetaInfo.getExtension(extensionName, extensionVersion);
      response = Collections.singleton(extensionInfo.convertToResponse());
    } else {
      try {
        Collection<ExtensionInfo> extensionInfos = ambariMetaInfo.getExtensions(extensionName);
        response = new HashSet<ExtensionVersionResponse>();
        for (ExtensionInfo extensionInfo: extensionInfos) {
          response.add(extensionInfo.convertToResponse());
        }
      } catch (StackAccessException e) {
        response = Collections.emptySet();
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
        String stackName    = request.getStackName();
        String stackVersion = request.getStackVersion();

        Set<RepositoryResponse> repositories = getRepositories(request);

        for (RepositoryResponse repositoryResponse : repositories) {
          if (repositoryResponse.getStackName() == null) {
            repositoryResponse.setStackName(stackName);
          }
          if (repositoryResponse.getStackVersion() == null) {
            repositoryResponse.setStackVersion(stackVersion);
          }

          repositoryResponse.setClusterVersionId(request.getClusterVersionId());
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
    Long repositoryVersionId = request.getRepositoryVersionId();
    String versionDefinitionId = request.getVersionDefinitionId();

    // !!! when asking for Repository responses for a versionDefinition, it is either for
    // an established repo version (a Long) OR from the in-memory generated ones (a String)
    if (null == repositoryVersionId && null != versionDefinitionId) {

      if (NumberUtils.isDigits(versionDefinitionId)) {
        repositoryVersionId = Long.valueOf(versionDefinitionId);
      }
    }

    Set<RepositoryResponse> responses = new HashSet<RepositoryResponse>();

    if (repositoryVersionId != null) {
      final RepositoryVersionEntity repositoryVersion = repositoryVersionDAO.findByPK(repositoryVersionId);
      if (repositoryVersion != null) {
        for (OperatingSystemEntity operatingSystem: repositoryVersion.getOperatingSystems()) {
          if (operatingSystem.getOsType().equals(osType)) {
            for (RepositoryEntity repository: operatingSystem.getRepositories()) {
              final RepositoryResponse response = new RepositoryResponse(repository.getBaseUrl(), osType, repository.getRepositoryId(), repository.getName(), "", "", "");
              if (null != versionDefinitionId) {
                response.setVersionDefinitionId(versionDefinitionId);
              } else {
                response.setRepositoryVersionId(repositoryVersionId);
              }
              response.setStackName(repositoryVersion.getStackName());
              response.setStackVersion(repositoryVersion.getStackVersion());
              responses.add(response);
            }
            break;
          }
        }
      }
    } else if (null != versionDefinitionId) {
      VersionDefinitionXml xml = ambariMetaInfo.getVersionDefinition(versionDefinitionId);

      if (null == xml) {
        throw new AmbariException(String.format("Version identified by %s does not exist",
            versionDefinitionId));
      }
      StackId stackId = new StackId(xml.release.stackId);

      ListMultimap<String, RepositoryInfo> stackRepositoriesByOs = ambariMetaInfo.getStackManager().getStack(stackName, stackVersion).getRepositoriesByOs();
      for (RepositoryXml.Os os : xml.repositoryInfo.getOses()) {

        for (RepositoryXml.Repo repo : os.getRepos()) {
          RepositoryResponse resp = new RepositoryResponse(repo.getBaseUrl(), os.getFamily(),
              repo.getRepoId(), repo.getRepoName(), repo.getMirrorsList(),
              repo.getBaseUrl(), repo.getLatestUri());

          resp.setVersionDefinitionId(versionDefinitionId);
          resp.setStackName(stackId.getStackName());
          resp.setStackVersion(stackId.getStackVersion());

          responses.add(resp);
        }
      }

      // Add service repos to the response. (These are not contained by the VDF but are present in the stack model)
      List<RepositoryInfo> serviceRepos =
          RepoUtil.getServiceRepos(xml.repositoryInfo.getRepositories(), stackRepositoriesByOs);
      responses.addAll(RepoUtil.asResponses(serviceRepos, versionDefinitionId, stackName, stackVersion));

    } else {
      if (repoId == null) {
        List<RepositoryInfo> repositories = ambariMetaInfo.getRepositories(stackName, stackVersion, osType);

        for (RepositoryInfo repository: repositories) {
          responses.add(repository.convertToResponse());
        }

      } else {
        RepositoryInfo repository = ambariMetaInfo.getRepository(stackName, stackVersion, osType, repoId);
        responses = Collections.singleton(repository.convertToResponse());
      }
    }

    return responses;
  }

  @Override
  public void updateRepositories(Set<RepositoryRequest> requests) throws AmbariException {
    for (RepositoryRequest rr : requests) {
      if (null == rr.getStackName() || rr.getStackName().isEmpty()) {
        throw new AmbariException("Stack name must be specified.");
      }

      if (null == rr.getStackVersion() || rr.getStackVersion().isEmpty()) {
        throw new AmbariException("Stack version must be specified.");
      }

      if (null == rr.getOsType() || rr.getOsType().isEmpty()) {
        throw new AmbariException("OS type must be specified.");
      }

      if (null == rr.getRepoId() || rr.getRepoId().isEmpty()) {
        throw new AmbariException("Repo ID must be specified.");
      }

      if (null == rr.getBaseUrl() && null == rr.getMirrorsList()) {
        throw new AmbariException("Repo Base Url or Mirrors List must be specified.");
      }

      if (rr.isVerifyBaseUrl()) {
        verifyRepository(rr);
      }
      if (rr.getRepositoryVersionId() != null) {
        throw new AmbariException("Can't directly update repositories in repository_version, update the repository_version instead");
      }
      ambariMetaInfo.updateRepo(rr.getStackName(), rr.getStackVersion(), rr.getOsType(), rr.getRepoId(), rr.getBaseUrl(), rr.getMirrorsList());

    }
  }

  @Override
  public void verifyRepositories(Set<RepositoryRequest> requests) throws AmbariException {
    for (RepositoryRequest request: requests) {
      if (request.getBaseUrl() == null) {
        throw new AmbariException("Base url is missing for request " + request);
      }
      verifyRepository(request);
    }
  }

  /**
   * Verifies single repository, see {{@link #verifyRepositories(Set)}.
   *
   * @param request request
   * @throws AmbariException if verification fails
   */
  private void verifyRepository(RepositoryRequest request) throws AmbariException {
    URLStreamProvider usp = new URLStreamProvider(REPO_URL_CONNECT_TIMEOUT, REPO_URL_READ_TIMEOUT, null, null, null);
    usp.setSetupTruststoreForHttps(false);

    RepositoryInfo repositoryInfo = ambariMetaInfo.getRepository(request.getStackName(), request.getStackVersion(), request.getOsType(), request.getRepoId());
    String repoName = repositoryInfo.getRepoName();

    String errorMessage = null;
    Exception e = null;

    String[] suffixes = configs.getRepoValidationSuffixes(request.getOsType());
    for (String suffix : suffixes) {
      String formatted_suffix = String.format(suffix, repoName);
      String spec = request.getBaseUrl().trim();

      // This logic is to identify if the end of baseurl has a slash ('/') and/or the beginning of suffix String (e.g. "/repodata/repomd.xml")
      // has a slash and they can form a good url.
      // e.g. "http://baseurl.com/" + "/repodata/repomd.xml" becomes "http://baseurl.com/repodata/repomd.xml" but not "http://baseurl.com//repodata/repomd.xml"
      if (spec.charAt(spec.length() - 1) != '/' && formatted_suffix.charAt(0) != '/') {
        spec = spec + "/" + formatted_suffix;
      } else if (spec.charAt(spec.length() - 1) == '/' && formatted_suffix.charAt(0) == '/') {
        spec = spec + formatted_suffix.substring(1);
      } else {
        spec = spec + formatted_suffix;
      }

      // if spec contains "file://" then check local file system.
      final String FILE_SCHEME = "file://";
      if(spec.toLowerCase().startsWith(FILE_SCHEME)){
        String filePath = spec.substring(FILE_SCHEME.length());
        File f = new File(filePath);
        if(!f.exists()){
          errorMessage = "Could not access base url . " + spec + " . ";
          e = new FileNotFoundException(errorMessage);
          break;
        }

      }else{
        try {
          IOUtils.readLines(usp.readFrom(spec));
        } catch (IOException ioe) {
          e = ioe;
          errorMessage = "Could not access base url . " + request.getBaseUrl() + " . ";
          if (LOG.isDebugEnabled()) {
            errorMessage += ioe;
          } else {
            errorMessage += ioe.getMessage();
          }
          break;
        }
      }
    }

    if (e != null) {
      LOG.error(errorMessage);
      throw new IllegalArgumentException(errorMessage, e);
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
      StackInfo stackInfo = ambariMetaInfo.getStack(stackName, stackVersion);
      response = Collections.singleton(stackInfo.convertToResponse());
    } else {
      try {
        Collection<StackInfo> stackInfos = ambariMetaInfo.getStacks(stackName);
        response = new HashSet<StackVersionResponse>();
        for (StackInfo stackInfo: stackInfos) {
          response.add(stackInfo.convertToResponse());
        }
      } catch (StackAccessException e) {
        response = Collections.emptySet();
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
      ServiceInfo service = ambariMetaInfo.getService(stackName, stackVersion, serviceName);
      response = Collections.singleton(new StackServiceResponse(service));
    } else {
      Map<String, ServiceInfo> services = ambariMetaInfo.getServices(stackName, stackVersion);
      response = new HashSet<StackServiceResponse>();
      for (ServiceInfo service : services.values()) {
        response.add(new StackServiceResponse(service));
      }
    }
    return response;
  }

  @Override
  public Set<StackConfigurationResponse> getStackLevelConfigurations(
      Set<StackLevelConfigurationRequest> requests) throws AmbariException {
    Set<StackConfigurationResponse> response = new HashSet<StackConfigurationResponse>();
    for (StackLevelConfigurationRequest request : requests) {

      String stackName    = request.getStackName();
      String stackVersion = request.getStackVersion();

      Set<StackConfigurationResponse> stackConfigurations = getStackLevelConfigurations(request);

      for (StackConfigurationResponse stackConfigurationResponse : stackConfigurations) {
        stackConfigurationResponse.setStackName(stackName);
        stackConfigurationResponse.setStackVersion(stackVersion);
      }

      response.addAll(stackConfigurations);
    }

    return response;
  }

  private Set<StackConfigurationResponse> getStackLevelConfigurations(
      StackLevelConfigurationRequest request) throws AmbariException {

    Set<StackConfigurationResponse> response = new HashSet<StackConfigurationResponse>();

    String stackName = request.getStackName();
    String stackVersion = request.getStackVersion();
    String propertyName = request.getPropertyName();

    Set<PropertyInfo> properties;
    if (propertyName != null) {
      properties = ambariMetaInfo.getStackPropertiesByName(stackName, stackVersion, propertyName);
    } else {
      properties = ambariMetaInfo.getStackProperties(stackName, stackVersion);
    }
    for (PropertyInfo property: properties) {
      response.add(property.convertToResponse());
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

    Set<StackConfigurationResponse> response = new HashSet<StackConfigurationResponse>();

    String stackName = request.getStackName();
    String stackVersion = request.getStackVersion();
    String serviceName = request.getServiceName();
    String propertyName = request.getPropertyName();

    Set<PropertyInfo> properties;
    if (propertyName != null) {
      properties = ambariMetaInfo.getPropertiesByName(stackName, stackVersion, serviceName, propertyName);
    } else {
      properties = ambariMetaInfo.getServiceProperties(stackName, stackVersion, serviceName);
    }
    for (PropertyInfo property: properties) {
      response.add(property.convertToResponse());
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
      ComponentInfo component = ambariMetaInfo.getComponent(stackName, stackVersion, serviceName, componentName);
      response = Collections.singleton(new StackServiceComponentResponse(
          component));

    } else {
      List<ComponentInfo> components = ambariMetaInfo.getComponentsByService(stackName, stackVersion, serviceName);
      response = new HashSet<StackServiceComponentResponse>();

      for (ComponentInfo component: components) {
        response.add(new StackServiceComponentResponse(component));
      }
    }
    return response;
  }

  @Override
  public Set<OperatingSystemResponse> getOperatingSystems(
      Set<OperatingSystemRequest> requests) throws AmbariException {
    Set<OperatingSystemResponse> response = new HashSet<OperatingSystemResponse>();
    for (OperatingSystemRequest request : requests) {
      try {
        String stackName    = request.getStackName();
        String stackVersion = request.getStackVersion();

        Set<OperatingSystemResponse> stackOperatingSystems = getOperatingSystems(request);

        for (OperatingSystemResponse operatingSystemResponse : stackOperatingSystems) {
          if (operatingSystemResponse.getStackName() == null) {
            operatingSystemResponse.setStackName(stackName);
          }
          if (operatingSystemResponse.getStackVersion() == null) {
            operatingSystemResponse.setStackVersion(stackVersion);
          }
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

  private Set<OperatingSystemResponse> getOperatingSystems(
      OperatingSystemRequest request) throws AmbariException {

    Set<OperatingSystemResponse> responses = new HashSet<OperatingSystemResponse>();

    String stackName = request.getStackName();
    String stackVersion = request.getStackVersion();
    String osType = request.getOsType();
    Long repositoryVersionId = request.getRepositoryVersionId();
    String versionDefinitionId = request.getVersionDefinitionId();

    // !!! when asking for OperatingSystem responses for a versionDefinition, it is either for
    // an established repo version (a Long) OR from the in-memory generated ones (a String)
    if (null == repositoryVersionId && null != versionDefinitionId) {
      if (NumberUtils.isDigits(versionDefinitionId)) {
        repositoryVersionId = Long.valueOf(versionDefinitionId);
      }
    }

    if (repositoryVersionId != null) {
      final RepositoryVersionEntity repositoryVersion = repositoryVersionDAO.findByPK(repositoryVersionId);
      if (repositoryVersion != null) {
        for (OperatingSystemEntity operatingSystem: repositoryVersion.getOperatingSystems()) {
          final OperatingSystemResponse response = new OperatingSystemResponse(operatingSystem.getOsType());
          if (null != versionDefinitionId) {
            response.setVersionDefinitionId(repositoryVersionId.toString());
          } else {
            response.setRepositoryVersionId(repositoryVersionId);
          }
          response.setStackName(repositoryVersion.getStackName());
          response.setStackVersion(repositoryVersion.getStackVersion());
          response.setAmbariManagedRepos(operatingSystem.isAmbariManagedRepos());
          responses.add(response);
        }
      }
    } else if (null != versionDefinitionId) {
      VersionDefinitionXml xml = ambariMetaInfo.getVersionDefinition(versionDefinitionId);

      if (null == xml) {
        throw new AmbariException(String.format("Version identified by %s does not exist",
            versionDefinitionId));
      }
      StackId stackId = new StackId(xml.release.stackId);

      for (RepositoryXml.Os os : xml.repositoryInfo.getOses()) {
        OperatingSystemResponse resp = new OperatingSystemResponse(os.getFamily());
        resp.setVersionDefinitionId(versionDefinitionId);
        resp.setStackName(stackId.getStackName());
        resp.setStackVersion(stackId.getStackVersion());

        responses.add(resp);
      }

    } else {
      if (osType != null) {
        OperatingSystemInfo operatingSystem = ambariMetaInfo.getOperatingSystem(stackName, stackVersion, osType);
        responses = Collections.singleton(operatingSystem.convertToResponse());
      } else {
        Set<OperatingSystemInfo> operatingSystems = ambariMetaInfo.getOperatingSystems(stackName, stackVersion);
        for (OperatingSystemInfo operatingSystem : operatingSystems) {
          responses.add(operatingSystem.convertToResponse());
        }
      }
    }

    return responses;
  }

  @Override
  public String getAuthName() {
    return AuthorizationHelper.getAuthenticatedName(configs.getAnonymousAuditName());
  }

  @Override
  public int getAuthId() {
    return AuthorizationHelper.getAuthenticatedId();
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
    return rootServiceResponseFactory.getRootServices(request);
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
    return rootServiceResponseFactory.getRootServiceComponents(request);
  }

  @Override
  public Clusters getClusters() {
    return clusters;
  }

  @Override
  public ConfigHelper getConfigHelper() {
    return configHelper;
  }

  @Override
  public AmbariMetaInfo getAmbariMetaInfo() {
    return ambariMetaInfo;
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
  public RoleGraphFactory getRoleGraphFactory() {
    return roleGraphFactory;
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

  @Override
  public Map<String, String> getRcaParameters() {

    String hostName = StageUtils.getHostName();

    String url = configs.getRcaDatabaseUrl();
    if (url.contains(Configuration.HOSTNAME_MACRO)) {
      url =
          url.replace(Configuration.HOSTNAME_MACRO,
              hostsMap.getHostMap(hostName));
    }

    Map<String, String> rcaParameters = new HashMap<String, String>();

    rcaParameters.put(AMBARI_DB_RCA_URL, url);
    rcaParameters.put(AMBARI_DB_RCA_DRIVER, configs.getRcaDatabaseDriver());
    rcaParameters.put(AMBARI_DB_RCA_USERNAME, configs.getRcaDatabaseUser());
    rcaParameters.put(AMBARI_DB_RCA_PASSWORD, configs.getRcaDatabasePassword());

    return rcaParameters;
  }

  @Override
  public boolean checkLdapConfigured() {
    return ldapDataPopulator.isLdapEnabled();
  }

  @Override
  public LdapSyncDto getLdapSyncInfo() throws AmbariException {
    return ldapDataPopulator.getLdapSyncInfo();
  }

  @Override
  public boolean isLdapSyncInProgress() {
    return ldapSyncInProgress;
  }

  @Override
  public synchronized LdapBatchDto synchronizeLdapUsersAndGroups(
      LdapSyncRequest userRequest, LdapSyncRequest groupRequest)
      throws AmbariException {
    ldapSyncInProgress = true;
    try {

      final LdapBatchDto batchInfo = new LdapBatchDto();

      if (userRequest != null) {
        switch (userRequest.getType()) {
          case ALL:
            ldapDataPopulator.synchronizeAllLdapUsers(batchInfo);
            break;
          case EXISTING:
            ldapDataPopulator.synchronizeExistingLdapUsers(batchInfo);
            break;
          case SPECIFIC:
            ldapDataPopulator.synchronizeLdapUsers(userRequest.getPrincipalNames(), batchInfo);
            break;
        }
      }
      if (groupRequest != null) {
        switch (groupRequest.getType()) {
          case ALL:
            ldapDataPopulator.synchronizeAllLdapGroups(batchInfo);
            break;
          case EXISTING:
            ldapDataPopulator.synchronizeExistingLdapGroups(batchInfo);
            break;
          case SPECIFIC:
            ldapDataPopulator.synchronizeLdapGroups(groupRequest.getPrincipalNames(), batchInfo);
            break;
        }
      }

      users.processLdapSync(batchInfo);
      return batchInfo;
    } finally {
      ldapSyncInProgress = false;
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void initializeWidgetsAndLayouts(Cluster cluster, Service service) throws AmbariException {
    StackId stackId = cluster.getDesiredStackVersion();
    Type widgetLayoutType = new TypeToken<Map<String, List<WidgetLayout>>>(){}.getType();

    try {
      Map<String, Object> widgetDescriptor = null;
      StackInfo stackInfo = ambariMetaInfo.getStack(stackId.getStackName(), stackId.getStackVersion());
      if (service != null) {
        // Service widgets
        ServiceInfo serviceInfo = stackInfo.getService(service.getName());
        File widgetDescriptorFile = serviceInfo.getWidgetsDescriptorFile();
        if (widgetDescriptorFile != null && widgetDescriptorFile.exists()) {
          try {
            widgetDescriptor = gson.fromJson(new FileReader(widgetDescriptorFile), widgetLayoutType);
          } catch (Exception ex) {
            String msg = "Error loading widgets from file: " + widgetDescriptorFile;
            LOG.error(msg, ex);
            throw new AmbariException(msg);
          }
        }
      } else {
        // Cluster level widgets
        String widgetDescriptorFileLocation = stackInfo.getWidgetsDescriptorFileLocation();
        if (widgetDescriptorFileLocation != null) {
          File widgetDescriptorFile = new File(widgetDescriptorFileLocation);
          if (widgetDescriptorFile.exists()) {
            try {
              widgetDescriptor = gson.fromJson(new FileReader(widgetDescriptorFile), widgetLayoutType);
            } catch (Exception ex) {
              String msg = "Error loading widgets from file: " + widgetDescriptorFile;
              LOG.error(msg, ex);
              throw new AmbariException(msg);
            }
          }
        }
      }
      if (widgetDescriptor != null) {
        LOG.debug("Loaded widget descriptor: " + widgetDescriptor);
        for (Object artifact : widgetDescriptor.values()) {
          List<WidgetLayout> widgetLayouts = (List<WidgetLayout>) artifact;
          createWidgetsAndLayouts(cluster, widgetLayouts);
        }
      }
    } catch (Exception e) {
      throw new AmbariException("Error creating stack widget artifacts. " +
        (service != null ? "Service: " + service.getName() + ", " : "") +
        "Cluster: " + cluster.getClusterName(), e);
    }
  }

  private WidgetEntity addIfNotExistsWidgetEntity(WidgetLayoutInfo layoutInfo, ClusterEntity clusterEntity,
                                          String user, long createTime) {
    List<WidgetEntity> createdEntities =
      widgetDAO.findByName(clusterEntity.getClusterId(), layoutInfo.getWidgetName(),
        user, layoutInfo.getDefaultSectionName());

    if (createdEntities == null || createdEntities.isEmpty()) {
      WidgetEntity widgetEntity = new WidgetEntity();
      widgetEntity.setClusterId(clusterEntity.getClusterId());
      widgetEntity.setClusterEntity(clusterEntity);
      widgetEntity.setScope(WidgetResourceProvider.SCOPE.CLUSTER.name());
      widgetEntity.setWidgetName(layoutInfo.getWidgetName());
      widgetEntity.setDefaultSectionName(layoutInfo.getDefaultSectionName());
      widgetEntity.setAuthor(user);
      widgetEntity.setDescription(layoutInfo.getDescription());
      widgetEntity.setTimeCreated(createTime);
      widgetEntity.setWidgetType(layoutInfo.getType());
      widgetEntity.setMetrics(gson.toJson(layoutInfo.getMetricsInfo()));
      widgetEntity.setProperties(gson.toJson(layoutInfo.getProperties()));
      widgetEntity.setWidgetValues(gson.toJson(layoutInfo.getValues()));
      widgetEntity.setListWidgetLayoutUserWidgetEntity(new LinkedList<WidgetLayoutUserWidgetEntity>());
      LOG.info("Creating cluster widget with: name = " +
        layoutInfo.getWidgetName() + ", type = " + layoutInfo.getType() + ", " +
        "cluster = " + clusterEntity.getClusterName());
      // Persisting not visible widgets
      // visible one will be cascaded on creation of layout
      if (!layoutInfo.isVisible()) {
        widgetDAO.create(widgetEntity);
      }
      return widgetEntity;
    } else {
      LOG.warn("Skip creating widget from stack artifact since one or more " +
        "already exits with name = " + layoutInfo.getWidgetName() + ", " +
          "clusterId = " + clusterEntity.getClusterId() + ", user = " + user);
    }
    return null;
  }

  @Transactional
  void createWidgetsAndLayouts(Cluster cluster, List<WidgetLayout> widgetLayouts) {
    String user = "ambari";
    Long clusterId = cluster.getClusterId();
    ClusterEntity clusterEntity = clusterDAO.findById(clusterId);
    if (clusterEntity == null) {
      return;
    }
    Long now = System.currentTimeMillis();

    if (widgetLayouts != null) {
      for (WidgetLayout widgetLayout : widgetLayouts) {
        List<WidgetLayoutEntity> existingEntities =
          widgetLayoutDAO.findByName(clusterId, widgetLayout.getLayoutName(), user);
        // Update layout properties if the layout exists
        if (existingEntities == null || existingEntities.isEmpty()) {
          WidgetLayoutEntity layoutEntity = new WidgetLayoutEntity();
          layoutEntity.setClusterEntity(clusterEntity);
          layoutEntity.setClusterId(clusterId);
          layoutEntity.setLayoutName(widgetLayout.getLayoutName());
          layoutEntity.setDisplayName(widgetLayout.getDisplayName());
          layoutEntity.setSectionName(widgetLayout.getSectionName());
          layoutEntity.setScope(WidgetLayoutResourceProvider.SCOPE.CLUSTER.name());
          layoutEntity.setUserName(user);

          List<WidgetLayoutUserWidgetEntity> widgetLayoutUserWidgetEntityList = new LinkedList<WidgetLayoutUserWidgetEntity>();
          int order = 0;
          for (WidgetLayoutInfo layoutInfo : widgetLayout.getWidgetLayoutInfoList()) {
            if (layoutInfo.getDefaultSectionName() == null) {
              layoutInfo.setDefaultSectionName(layoutEntity.getSectionName());
            }
            WidgetEntity widgetEntity = addIfNotExistsWidgetEntity(layoutInfo, clusterEntity, user, now);
            // Add to layout if visibility is true and widget was newly added
            if (widgetEntity != null && layoutInfo.isVisible()) {
              WidgetLayoutUserWidgetEntity widgetLayoutUserWidgetEntity = new WidgetLayoutUserWidgetEntity();
              widgetLayoutUserWidgetEntity.setWidget(widgetEntity);
              widgetLayoutUserWidgetEntity.setWidgetOrder(order++);
              widgetLayoutUserWidgetEntity.setWidgetLayout(layoutEntity);
              widgetLayoutUserWidgetEntityList.add(widgetLayoutUserWidgetEntity);
              widgetEntity.getListWidgetLayoutUserWidgetEntity().add(widgetLayoutUserWidgetEntity);
            }
          }
          layoutEntity.setListWidgetLayoutUserWidgetEntity(widgetLayoutUserWidgetEntityList);
          widgetLayoutDAO.createWithFlush(layoutEntity);
        } else {
          if (existingEntities.size() > 1) {
            LOG.warn("Skip updating layout since multiple widget layouts " +
              "found with: name = " + widgetLayout.getLayoutName() + ", " +
              "user = " + user + ", cluster = " + cluster.getClusterName());
          } else {
            WidgetLayoutEntity existingLayoutEntity = existingEntities.iterator().next();
            existingLayoutEntity.setSectionName(widgetLayout.getSectionName());
            existingLayoutEntity.setDisplayName(widgetLayout.getDisplayName());
            // Add new widgets to end of the existing ones
            List<WidgetLayoutUserWidgetEntity> layoutUserWidgetEntities = existingLayoutEntity.getListWidgetLayoutUserWidgetEntity();
            if (layoutUserWidgetEntities == null) {
              layoutUserWidgetEntities = new LinkedList<WidgetLayoutUserWidgetEntity>();
              existingLayoutEntity.setListWidgetLayoutUserWidgetEntity(layoutUserWidgetEntities);
            }
            int order = layoutUserWidgetEntities.size() - 1;
            List<WidgetLayoutInfo> layoutInfoList = widgetLayout.getWidgetLayoutInfoList();
            if (layoutInfoList != null && !layoutInfoList.isEmpty()) {
              for (WidgetLayoutInfo layoutInfo : layoutInfoList) {
                WidgetEntity widgetEntity = addIfNotExistsWidgetEntity(layoutInfo, clusterEntity, user, now);
                if (widgetEntity != null && layoutInfo.isVisible()) {
                  WidgetLayoutUserWidgetEntity widgetLayoutUserWidgetEntity = new WidgetLayoutUserWidgetEntity();
                  widgetLayoutUserWidgetEntity.setWidget(widgetEntity);
                  widgetLayoutUserWidgetEntity.setWidgetOrder(order++);
                  widgetLayoutUserWidgetEntity.setWidgetLayout(existingLayoutEntity);
                  layoutUserWidgetEntities.add(widgetLayoutUserWidgetEntity);
                  widgetEntity.getListWidgetLayoutUserWidgetEntity().add(widgetLayoutUserWidgetEntity);
                }
              }
            }
            widgetLayoutDAO.mergeWithFlush(existingLayoutEntity);
          }
        }
      }
    }
  }

  @Override
  public TimelineMetricCacheProvider getTimelineMetricCacheProvider() {
    return injector.getInstance(TimelineMetricCacheProvider.class);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetricPropertyProviderFactory getMetricPropertyProviderFactory() {
    return injector.getInstance(MetricPropertyProviderFactory.class);
  }

  @Override
  public LoggingSearchPropertyProvider getLoggingSearchPropertyProvider() {
    return injector.getInstance(LoggingSearchPropertyProvider.class);
  }

  @Override
  public LoggingService getLoggingService(String clusterName) {
    LoggingService loggingService = new LoggingService(clusterName);
    injector.injectMembers(loggingService);
    return loggingService;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AmbariEventPublisher getAmbariEventPublisher() {
    return injector.getInstance(AmbariEventPublisher.class);
  }

  @Override
  public KerberosHelper getKerberosHelper() {
    return kerberosHelper;
  }

  @Override
  public CredentialStoreService getCredentialStoreService() {
    return credentialStoreService;
  }

  /**
   * Queries the CredentialStoreService to gather properties about it.
   * <p/>
   * In particular, the details about which storage facilities are avaialble are returned via Boolean
   * properties.
   *
   * @return a map of properties
   */
  public Map<String,String> getCredentialStoreServiceProperties() {
    Map<String,String> properties = new HashMap<String, String>();
    properties.put("storage.persistent", String.valueOf(credentialStoreService.isInitialized(CredentialStoreType.PERSISTED)));
    properties.put("storage.temporary", String.valueOf(credentialStoreService.isInitialized(CredentialStoreType.TEMPORARY)));
    return properties;
  }

  @Override
  public MetricsCollectorHAManager getMetricsCollectorHAManager() {
    return injector.getInstance(MetricsCollectorHAManager.class);
  }

  /**
   * Validates that the authenticated user can set a service's (run-as) user and group.
   * <p/>
   * If the user is authorized to set service users and groups, than this method exits quickly.
   * If the user is not authorized to set service users and groups, then this method verifies that
   * the properties of types USER and GROUP have not been changed. If they have been, an
   * AuthorizationException is thrown.
   *
   * @param cluster         the relevant cluster
   * @param configType      the changed configuration type
   * @param propertyChanges a map of the property changes for the relevant configuration type
   * @throws AuthorizationException if the user is not authorized to perform this operation
   */
  protected void validateAuthorizationToUpdateServiceUsersAndGroups(Cluster cluster,
                                                                    String configType,
                                                                    Map<String, String[]> propertyChanges)
      throws AuthorizationException {

    if ((propertyChanges != null) && !propertyChanges.isEmpty()) {
      // If the authenticated user is not authorized to set service users or groups, make sure the
      // relevant properties are not changed. However, if the user is authorized to set service
      // users and groups, there is nothing to check.
      if (!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, cluster.getResourceId(),
          RoleAuthorization.SERVICE_SET_SERVICE_USERS_GROUPS)) {

        Map<PropertyInfo.PropertyType, Set<String>> propertyTypes = cluster.getConfigPropertiesTypes(configType);

        //  Create a composite set of properties to check...
        Set<String> propertiesToCheck = new HashSet<String>();

        Set<String> userProperties = propertyTypes.get(PropertyType.USER);
        if (userProperties != null) {
          propertiesToCheck.addAll(userProperties);
        }

        Set<String> groupProperties = propertyTypes.get(PropertyType.GROUP);
        if (groupProperties != null) {
          propertiesToCheck.addAll(groupProperties);
        }

        // If there are no USER or GROUP type properties, skip the validation check...
        for (String propertyName : propertiesToCheck) {
          String[] values = propertyChanges.get(propertyName);
          if (values != null) {
            String existingValue = values[0];
            String requestedValue = values[1];

            // If the properties don't match, so thrown an authorization exception
            if ((existingValue == null) ? (requestedValue != null) : !existingValue.equals(requestedValue)) {
              throw new AuthorizationException("The authenticated user is not authorized to set service user and groups");
            }
          }
        }
      }
    }
  }

  /**
   * Validates that the authenticated user can manage the cluster-wide configuration for a service's
   * ability to be set to auto-start.
   * <p/>
   * If the user is authorized, than this method exits quickly.
   * If the user is not authorized, then this method verifies that the configuration property
   * <code>cluster-env/recovery_enabled</code> is not changed. If it was, an
   * {@link AuthorizationException} is thrown.
   *
   * @param cluster         the relevant cluster
   * @param configType      the changed configuration type
   * @param propertyChanges a map of the property changes for the relevant configuration type
   * @throws AuthorizationException if the user is not authorized to perform this operation
   */
  protected void validateAuthorizationToManageServiceAutoStartConfiguration(Cluster cluster,
                                                                            String configType,
                                                                            Map<String, String[]> propertyChanges)
      throws AuthorizationException {
    // If the authenticated user is authorized to manage the cluster-wide configuration for a
    // service's ability to be set to auto-start, there is nothing to check.
    if (!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, cluster.getResourceId(),
        RoleAuthorization.CLUSTER_MANAGE_AUTO_START)) {

      if ("cluster-env".equals(configType) && propertyChanges.containsKey("recovery_enabled")) {
        throw new AuthorizationException("The authenticated user is not authorized to set service user and groups");
      }
    }
  }

  /**
   * Validates that the authenticated user can modify configurations for either a service or the
   * cluster.
   * <p>
   * Since some properties have special meaning, they may be ignored when perfoming this authorization
   * check. For example, to change the cluster's overall auto-start setting (cluster-env/recovery_enabled)
   * requires a specific permission that is not the same as the ability to set cluster-wide properties
   * (in general).  Because of this, the <code>cluster-env/recovery_enabled</code> propery should be
   * ignored in this check since permission to change it is expected to be validated elsewhere.
   *
   * @param cluster                the relevant cluster
   * @param configType             the changed configuration type
   * @param propertyChanges        a map of the property changes for the relevant configuration type
   * @param changesToIgnore        a map of configuration type names to sets of propery names to be ignored
   * @param isServiceConfiguration <code>true</code>, if the configuration type is a service-level configuration;
   *                               <code>false</code>, if the configuration type is a cluster-level configuration
   * @throws AuthorizationException if the authenticated user is not authorized to change the requested configuration
   */
  private void validateAuthorizationToModifyConfigurations(Cluster cluster, String configType,
                                                           Map<String, String[]> propertyChanges,
                                                           Map<String, Set<String>> changesToIgnore,
                                                           boolean isServiceConfiguration)
      throws AuthorizationException {
    // If the authenticated user is authorized to update cluster-wide/service-level configurations
    // there is nothing to check, else ensure no (relevant) configurations are being changed - ignoring
    // the specified configurations which may fall under a special category.
    // For example cluster-env/recovery_enabled requires a special permission - CLUSTER.MANAGE_AUTO_START
    if ((propertyChanges != null) && !propertyChanges.isEmpty()) {
      boolean isAuthorized = (isServiceConfiguration)
          ? AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, cluster.getResourceId(), RoleAuthorization.SERVICE_MODIFY_CONFIGS)
          : AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, cluster.getResourceId(), RoleAuthorization.CLUSTER_MODIFY_CONFIGS);

      if (!isAuthorized) {
        Set<String> relevantChangesToIgnore = changesToIgnore.get(configType);
        Map<String, String[]> relevantPropertyChanges;

        // If necessary remove any non-relevant property changes.
        if (relevantChangesToIgnore == null)
          relevantPropertyChanges = propertyChanges;
        else {
          relevantPropertyChanges = new HashMap<String, String[]>(propertyChanges);

          for (String propertyName : relevantChangesToIgnore) {
            relevantPropertyChanges.remove(propertyName);
          }
        }

        // If relevant configuration changes have been made, then the user is not authorized to
        // perform the requested operation and an AuthorizationException must be thrown
        if (relevantPropertyChanges.size() > 0) {
          throw new AuthorizationException(String.format("The authenticated user does not have authorization to modify %s configurations",
              (isServiceConfiguration) ? "service" : "cluster"));
        }
      }
    }
  }

  /**
   * This method will delete a link between an extension version and a stack version (Extension Link).
   *
   * An extension version is like a stack version but it contains custom services.  Linking an extension
   * version to the current stack version allows the cluster to install the custom services contained in
   * the extension version.
   */
  @Override
  public void deleteExtensionLink(ExtensionLinkRequest request) throws AmbariException {
    if (request.getLinkId() == null) {
      throw new IllegalArgumentException("Link ID should be provided");
    }
    ExtensionLinkEntity linkEntity = null;
    try {
      linkEntity = linkDAO.findById(new Long(request.getLinkId()));
    } catch (RollbackException e) {
      throw new AmbariException("Unable to find extension link"
            + ", linkId=" + request.getLinkId(), e);
    }

    StackInfo stackInfo = ambariMetaInfo.getStack(linkEntity.getStack().getStackName(), linkEntity.getStack().getStackVersion());

    if (stackInfo == null) {
      throw new StackAccessException("stackName=" + linkEntity.getStack().getStackName() + ", stackVersion=" + linkEntity.getStack().getStackVersion());
    }

    ExtensionInfo extensionInfo = ambariMetaInfo.getExtension(linkEntity.getExtension().getExtensionName(), linkEntity.getExtension().getExtensionVersion());

    if (extensionInfo == null) {
      throw new StackAccessException("extensionName=" + linkEntity.getExtension().getExtensionName() + ", extensionVersion=" + linkEntity.getExtension().getExtensionVersion());
    }

    ExtensionHelper.validateDeleteLink(getClusters(), stackInfo, extensionInfo);
    ambariMetaInfo.getStackManager().unlinkStackAndExtension(stackInfo, extensionInfo);

    try {
      linkDAO.remove(linkEntity);
    } catch (RollbackException e) {
      throw new AmbariException("Unable to delete extension link"
              + ", linkId=" + request.getLinkId()
              + ", stackName=" + request.getStackName()
              + ", stackVersion=" + request.getStackVersion()
              + ", extensionName=" + request.getExtensionName()
              + ", extensionVersion=" + request.getExtensionVersion(), e);
    }
  }

  /**
   * This method will create a link between an extension version and a stack version (Extension Link).
   *
   * An extension version is like a stack version but it contains custom services.  Linking an extension
   * version to the current stack version allows the cluster to install the custom services contained in
   * the extension version.
   */
  @Override
  public void createExtensionLink(ExtensionLinkRequest request) throws AmbariException {
    validateCreateExtensionLinkRequest(request);

    StackInfo stackInfo = ambariMetaInfo.getStack(request.getStackName(), request.getStackVersion());

    if (stackInfo == null) {
      throw new StackAccessException("stackName=" + request.getStackName() + ", stackVersion=" + request.getStackVersion());
    }

    ExtensionInfo extensionInfo = ambariMetaInfo.getExtension(request.getExtensionName(), request.getExtensionVersion());

    if (extensionInfo == null) {
      throw new StackAccessException("extensionName=" + request.getExtensionName() + ", extensionVersion=" + request.getExtensionVersion());
    }

    ExtensionHelper.validateCreateLink(stackInfo, extensionInfo);
    ExtensionLinkEntity linkEntity = createExtensionLinkEntity(request);
    ambariMetaInfo.getStackManager().linkStackToExtension(stackInfo, extensionInfo);

    try {
      linkDAO.create(linkEntity);
      linkEntity = linkDAO.merge(linkEntity);
    } catch (RollbackException e) {
      String message = "Unable to create extension link";
      LOG.debug(message, e);
      String errorMessage = message
              + ", stackName=" + request.getStackName()
              + ", stackVersion=" + request.getStackVersion()
              + ", extensionName=" + request.getExtensionName()
              + ", extensionVersion=" + request.getExtensionVersion();
      LOG.warn(errorMessage);
      throw new AmbariException(errorMessage, e);
    }
  }

  /**
   * This method will update a link between an extension version and a stack version (Extension Link).
   * Updating will only force ambari server to reread the stack and extension directories.
   *
   * An extension version is like a stack version but it contains custom services.  Linking an extension
   * version to the current stack version allows the cluster to install the custom services contained in
   * the extension version.
   */
  @Override
  public void updateExtensionLink(ExtensionLinkRequest request) throws AmbariException {
    if (request.getLinkId() == null) {
      throw new AmbariException("Link ID should be provided");
    }
    ExtensionLinkEntity linkEntity = null;
    try {
      linkEntity = linkDAO.findById(new Long(request.getLinkId()));
    } catch (RollbackException e) {
      throw new AmbariException("Unable to find extension link"
            + ", linkId=" + request.getLinkId(), e);
    }
    updateExtensionLink(linkEntity);
  }

  /**
   * This method will update a link between an extension version and a stack version (Extension Link).
   * Updating will only force ambari server to reread the stack and extension directories.
   *
   * An extension version is like a stack version but it contains custom services.  Linking an extension
   * version to the current stack version allows the cluster to install the custom services contained in
   * the extension version.
   */
  @Override
  public void updateExtensionLink(ExtensionLinkEntity linkEntity) throws AmbariException {
    StackInfo stackInfo = ambariMetaInfo.getStack(linkEntity.getStack().getStackName(), linkEntity.getStack().getStackVersion());

    if (stackInfo == null) {
      throw new StackAccessException("stackName=" + linkEntity.getStack().getStackName() + ", stackVersion=" + linkEntity.getStack().getStackVersion());
    }

    ExtensionInfo extensionInfo = ambariMetaInfo.getExtension(linkEntity.getExtension().getExtensionName(), linkEntity.getExtension().getExtensionVersion());

    if (extensionInfo == null) {
      throw new StackAccessException("extensionName=" + linkEntity.getExtension().getExtensionName() + ", extensionVersion=" + linkEntity.getExtension().getExtensionVersion());
    }

    ambariMetaInfo.getStackManager().linkStackToExtension(stackInfo, extensionInfo);
  }

  private void validateCreateExtensionLinkRequest(ExtensionLinkRequest request) throws AmbariException {
    if (request.getStackName() == null
            || request.getStackVersion() == null
            || request.getExtensionName() == null
            || request.getExtensionVersion() == null) {

      throw new IllegalArgumentException("Stack name, stack version, extension name and extension version should be provided");
    }

    ExtensionLinkEntity entity = linkDAO.findByStackAndExtension(request.getStackName(), request.getStackVersion(),
            request.getExtensionName(), request.getExtensionVersion());

    if (entity != null) {
      throw new AmbariException("The stack and extension are already linked"
                + ", stackName=" + request.getStackName()
                + ", stackVersion=" + request.getStackVersion()
                + ", extensionName=" + request.getExtensionName()
                + ", extensionVersion=" + request.getExtensionVersion());
    }
  }

  private ExtensionLinkEntity createExtensionLinkEntity(ExtensionLinkRequest request) throws AmbariException {
    StackEntity stack = stackDAO.find(request.getStackName(), request.getStackVersion());
    ExtensionEntity extension = extensionDAO.find(request.getExtensionName(), request.getExtensionVersion());

    ExtensionLinkEntity linkEntity = new ExtensionLinkEntity();
    linkEntity.setStack(stack);
    linkEntity.setExtension(extension);
    return linkEntity;
  }
}
