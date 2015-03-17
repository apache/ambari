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

import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.AMBARI_DB_RCA_DRIVER;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.AMBARI_DB_RCA_PASSWORD;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.AMBARI_DB_RCA_URL;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.AMBARI_DB_RCA_USERNAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.CLIENTS_TO_UPDATE_CONFIGS;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.COMMAND_TIMEOUT;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.DB_DRIVER_FILENAME;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.GROUP_LIST;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.HOOKS_FOLDER;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.PACKAGE_LIST;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.REPO_INFO;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SCRIPT;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SCRIPT_TYPE;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SERVICE_PACKAGE_FOLDER;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SERVICE_REPO_INFO;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.USER_LIST;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.VERSION;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
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
import org.apache.ambari.server.configuration.Configuration.DatabaseType;
import org.apache.ambari.server.controller.internal.RequestOperationLevel;
import org.apache.ambari.server.controller.internal.RequestResourceFilter;
import org.apache.ambari.server.controller.internal.RequestStageContainer;
import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.customactions.ActionDefinition;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.metadata.RoleCommandOrder;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.OperatingSystemEntity;
import org.apache.ambari.server.orm.entities.RepositoryEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.scheduler.ExecutionScheduleManager;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.Group;
import org.apache.ambari.server.security.authorization.User;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.security.ldap.AmbariLdapDataPopulator;
import org.apache.ambari.server.security.ldap.LdapBatchDto;
import org.apache.ambari.server.security.ldap.LdapSyncDto;
import org.apache.ambari.server.serveraction.kerberos.KerberosInvalidConfigurationException;
import org.apache.ambari.server.serveraction.kerberos.KerberosOperationException;
import org.apache.ambari.server.stageplanner.RoleGraph;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.CommandScriptDefinition;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostComponentAdminState;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.OperatingSystemInfo;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.PropertyInfo.PropertyType;
import org.apache.ambari.server.state.RepositoryInfo;
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
import org.apache.ambari.server.state.scheduler.RequestExecutionFactory;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostInstallEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStartEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStopEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostUpgradeEvent;
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

  private static final String BASE_LOG_DIR = "/tmp/ambari";

  private final Clusters clusters;

  private final ActionManager actionManager;

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
  @Inject
  private AmbariLdapDataPopulator ldapDataPopulator;
  @Inject
  private RepositoryVersionDAO repositoryVersionDAO;

  private MaintenanceStateHelper maintenanceStateHelper;

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
    uriBuilder.setPath(path);

    return uriBuilder.toString();
  }

  @Override
  public RoleCommandOrder getRoleCommandOrder(Cluster cluster) {
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
    StackInfo stackInfo = ambariMetaInfo.getStack(stackId.getStackName(),
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

      setRestartRequiredServices(s, request.getHostname());

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

  @Transactional
  void persistServiceComponentHosts(Set<ServiceComponentHostRequest> requests)
    throws AmbariException {

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

      sc.addServiceComponentHost(sch);
      sch.persist();
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
          Service service, String hostName) throws AmbariException {

    Cluster cluster = service.getCluster();
    StackId stackId = cluster.getCurrentStackVersion();
    Set<String> needRestartServices = ambariMetaInfo.getRestartRequiredServicesNames(
        stackId.getStackName(), stackId.getStackVersion());

    if(needRestartServices.contains(service.getName())) {
      Map<String, ServiceComponent> m = service.getServiceComponents();
      for (Entry<String, ServiceComponent> entry : m.entrySet()) {
        ServiceComponent serviceComponent = entry.getValue();
        if (serviceComponent.isMasterComponent()) {
          Map<String, ServiceComponentHost> schMap = serviceComponent.getServiceComponentHosts();
          //schMap will only contain hostName when deleting a host; when adding a host the test skips the loop
          if(schMap.containsKey(hostName)) {
            for (Entry<String, ServiceComponentHost> sch : schMap.entrySet()) {
              ServiceComponentHost serviceComponentHost = sch.getValue();
              serviceComponentHost.setRestartRequired(true);
            }
          }
        }
      }
    }
  }


  @Override
  public synchronized ConfigurationResponse createConfiguration(
      ConfigurationRequest request) throws AmbariException {
    if (null == request.getClusterName() || request.getClusterName().isEmpty()
        || null == request.getType() || request.getType().isEmpty()
        || null == request.getProperties()) {
      throw new IllegalArgumentException("Invalid Arguments,"
          + " clustername, config type and configs should not"
          + " be null or empty");
    }

    Cluster cluster = clusters.getCluster(request.getClusterName());

    Map<String, Config> configs = cluster.getConfigsByType(
        request.getType());
    if (null == configs) {
      configs = new HashMap<String, Config>();
    }

    // Configuration attributes are optional. If not present, use empty map
    Map<String, Map<String, String>> propertiesAttributes = request.getPropertiesAttributes();
    if (null == propertiesAttributes) {
      propertiesAttributes = new HashMap<String, Map<String,String>>();
    }

    if (configs.containsKey(request.getVersionTag())) {
      throw new AmbariException(MessageFormat.format("Configuration with tag ''{0}'' exists for ''{1}''",
          request.getVersionTag(),
          request.getType()));
    }

    handleGlobalsBackwardsCompability(request, propertiesAttributes);

    Config config = createConfig(cluster, request.getType(), request.getProperties(),
        request.getVersionTag(), propertiesAttributes);

    return new ConfigurationResponse(cluster.getClusterName(), config.getType(), config.getTag(), config.getVersion(),
        config.getProperties(), config.getPropertiesAttributes());
  }

  private void handleGlobalsBackwardsCompability(ConfigurationRequest request,
      Map<String, Map<String, String>> propertiesAttributes) throws AmbariException {
    Cluster cluster = clusters.getCluster(request.getClusterName());
    if(request.getType().equals(Configuration.GLOBAL_CONFIG_TAG)) {
      Map<String, Map<String, String>> configTypes = new HashMap<String, Map<String, String>>();
      configTypes.put(Configuration.GLOBAL_CONFIG_TAG, request.getProperties());
      configHelper.moveDeprecatedGlobals(cluster.getCurrentStackVersion(), configTypes, cluster.getClusterName());

      for(Map.Entry<String, Map<String, String>> configType : configTypes.entrySet()) {
        String configTypeName = configType.getKey();
        Map<String, String> properties = configType.getValue();

        if(configTypeName.equals(Configuration.GLOBAL_CONFIG_TAG)) {
          continue;
        }

        String tag;
        if(cluster.getConfigsByType(configTypeName) == null) {
          tag = "version1";
        } else {
          tag = "version" + System.currentTimeMillis();
        }

        Config config = createConfig(cluster, configTypeName, properties, tag, propertiesAttributes);

        if (config != null) {
          String authName = getAuthName();

          if (cluster.addDesiredConfig(authName, Collections.singleton(config)) != null) {
            LOG.info("cluster '" + cluster.getClusterName() + "' "
                    + "changed by: '" + authName + "'; "
                    + "type='" + config.getType() + "' "
                    + "tag='" + config.getTag());
          }
        }

      }
    }
  }

  private Config createConfig(Cluster cluster, String type, Map<String, String> properties,
      String versionTag, Map<String, Map<String, String>> propertiesAttributes) {
    Config config = configFactory.createNew (cluster, type,
        properties, propertiesAttributes);

    if (!StringUtils.isEmpty(versionTag)) {
      config.setTag(versionTag);
    }

    config.persist();

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

      users.createUser(request.getUsername(), request.getPassword(), request.isActive(), request.isAdmin(), false);
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
      users.createGroup(request.getGroupName());
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
      throws AmbariException {

    Set<ClusterResponse> response = new HashSet<ClusterResponse>();

    if (LOG.isDebugEnabled()) {
      LOG.debug("Received a getClusters request"
          + ", clusterName=" + request.getClusterName()
          + ", clusterId=" + request.getClusterId()
          + ", stackInfo=" + request.getStackVersion());
    }

    Cluster singleCluster = null;
    if (request.getClusterName() != null) {
      singleCluster = clusters.getCluster(request.getClusterName());
    } else if (request.getClusterId() != null) {
      singleCluster = clusters.getClusterById(request.getClusterId());
    }

    if (singleCluster != null) {
      ClusterResponse cr = singleCluster.convertToResponse();
      cr.setDesiredConfigs(singleCluster.getDesiredConfigs());
      cr.setDesiredServiceConfigVersions(singleCluster.getActiveServiceConfigVersions());
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
              throw new ServiceComponentHostNotFoundException(cluster.getClusterName(),
                s.getName(), sc.getName(), request.getHostname());
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
            cluster.getClusterName(), config.getType(), config.getTag(), config.getVersion(),
            config.getProperties(), config.getPropertiesAttributes());
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
                cluster.getClusterName(), request.getType(),
                config.getTag(), entry.getValue().getVersion(),
                includeProps ? config.getProperties() : new HashMap<String, String>(),
                includeProps ? config.getPropertiesAttributes() : new HashMap<String, Map<String,String>>());
            responses.add(response);
          }
        }
      } else {
        // !!! all configuration
        Collection<Config> all = cluster.getAllConfigs();

        for (Config config : all) {
          ConfigurationResponse response = new ConfigurationResponse(
              cluster.getClusterName(), config.getType(), config.getTag(), config.getVersion(),
              includeProps ? config.getProperties() : new HashMap<String, String>(),
              includeProps ? config.getPropertiesAttributes() : new HashMap<String, Map<String,String>>());

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

      cluster.addSessionAttributes(request.getSessionAttributes());
      //
      // ***************************************************

      response = updateCluster(request, requestProperties);
    }
    return response;
  }

  private synchronized RequestStatusResponse updateCluster(ClusterRequest request, Map<String, String> requestProperties)
      throws AmbariException {

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
      cluster.setClusterName(request.getClusterName());
    }

    //check if desired configs are available in request and they were changed
    boolean isConfigurationCreationNeeded = false;
    if (request.getDesiredConfig() != null) {
      for (ConfigurationRequest desiredConfig : request.getDesiredConfig()) {
        Map<String, String> requestConfigProperties = desiredConfig.getProperties();
        Config clusterConfig = cluster.getDesiredConfigByType(desiredConfig.getType());
        Map<String, String> clusterConfigProperties = null;
        if (clusterConfig != null) {
          clusterConfigProperties = clusterConfig.getProperties();
        } else {
          isConfigurationCreationNeeded = true;
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
            for (Entry<String, String> property : requestConfigProperties.entrySet()) {
              if (!property.getValue().equals(clusterConfigProperties.get(property.getKey()))) {
                isConfigurationCreationNeeded =true;
                break;
              }
            }
          }
        }
      }
    }

    // set or create configuration mapping (and optionally create the map of properties)
    if (isConfigurationCreationNeeded) {
      Set<Config> configs = new HashSet<Config>();
      String note = null;
      for (ConfigurationRequest cr: request.getDesiredConfig()) {

      if (null != cr.getProperties()) {
        // !!! empty property sets are supported, and need to be able to use
        // previously-defined configs (revert)
        Map<String, Config> all = cluster.getConfigsByType(cr.getType());
        if (null == all ||                              // none set
            !all.containsKey(cr.getVersionTag()) ||     // tag not set
            cr.getProperties().size() > 0) {            // properties to set

          LOG.info(MessageFormat.format("Applying configuration with tag ''{0}'' to cluster ''{1}''",
              cr.getVersionTag(),
              request.getClusterName()));

          cr.setClusterName(cluster.getClusterName());
          configurationResponses.add(createConfiguration(cr));
        }
      }
        note = cr.getServiceConfigVersionNote();
        configs.add(cluster.getConfig(cr.getType(), cr.getVersionTag()));
      }
      if (!configs.isEmpty()) {
        String authName = getAuthName();
        serviceConfigVersionResponse = cluster.addDesiredConfig(authName, configs, note);
        if (serviceConfigVersionResponse != null) {
          Logger logger = LoggerFactory.getLogger("configchange");
          for (Config config: configs) {
            logger.info("cluster '" + request.getClusterName() + "' "
                + "changed by: '" + authName + "'; "
                + "type='" + config.getType() + "' "
                + "tag='" + config.getTag() + "'");
          }
        }
      }
    }

    StackId currentVersion = cluster.getCurrentStackVersion();
    StackId desiredVersion = cluster.getDesiredStackVersion();

    // Set the current version value if its not already set
    if (currentVersion == null) {
      cluster.setCurrentStackVersion(desiredVersion);
    }
    // Rolling Upgrade: unlike the workflow for creating a cluster, updating a cluster via the API will not
    // create any ClusterVersionEntity changes because those have to go through the Rolling Upgrade process.

    boolean requiresHostListUpdate =
        request.getHostNames() != null && !request.getHostNames().isEmpty();

    if (requiresHostListUpdate) {
      clusters.mapHostsToCluster(
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
        try {
          requestStageContainer = kerberosHelper.executeCustomOperations(cluster, requestProperties, requestStageContainer);
        } catch (KerberosOperationException e) {
          throw new IllegalArgumentException(e.getMessage(), e);
        }
      } else if (cluster.getSecurityType() != securityType) {
        LOG.info("Received cluster security type change request from {} to {}",
            cluster.getSecurityType().name(), securityType.name());

        if ((securityType == SecurityType.KERBEROS) || (securityType == SecurityType.NONE)) {
          // Since the security state of the cluster has changed, invoke toggleKerberos to handle
          // adding or removing Kerberos from the cluster. This may generate multiple stages
          // or not depending the current state of the cluster.
          try {
            requestStageContainer = kerberosHelper.toggleKerberos(cluster, securityType, requestStageContainer);
          } catch (KerberosOperationException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
          }
        } else {
          throw new IllegalArgumentException(String.format("Unexpected security type encountered: %s", securityType.name()));
        }

        cluster.setSecurityType(securityType);
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
                                Stage stage, ServiceComponentHost scHost,
                                Map<String, Map<String, String>> configurations,
                                Map<String, Map<String, Map<String, String>>> configurationAttributes,
                                Map<String, Map<String, String>> configTags,
                                RoleCommand roleCommand,
                                Map<String, String> commandParams,
                                ServiceComponentHostEvent event
                                )
                                throws AmbariException {

    stage.addHostRoleExecutionCommand(scHost.getHostName(), Role.valueOf(scHost
      .getServiceComponentName()), roleCommand,
      event, scHost.getClusterName(),
      scHost.getServiceName(), false);
    String serviceName = scHost.getServiceName();
    String componentName = event.getServiceComponentName();
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

    String jobtrackerHost = getJobTrackerHost(cluster);
    if (!scHost.getHostName().equals(jobtrackerHost)) {
      if (configTags.get(Configuration.GLOBAL_CONFIG_TAG) != null) {
        configHelper.applyCustomConfig(
          configurations, Configuration.GLOBAL_CONFIG_TAG,
          Configuration.RCA_ENABLED_PROPERTY, "false", false);
      }
    }

    execCmd.setConfigurations(configurations);
    execCmd.setConfigurationAttributes(configurationAttributes);
    execCmd.setConfigurationTags(configTags);
    if (commandParams == null) { // if not defined
      commandParams = new TreeMap<String, String>();
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
        ClusterVersionEntity currentClusterVersion = cluster.getCurrentClusterVersion();
        if (currentClusterVersion != null) {
         commandParams.put(VERSION, currentClusterVersion.getRepositoryVersion().getVersion());
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
    hostParams.putAll(getRcaParameters());

    List<ServiceOsSpecific.Package> packages =
            getPackagesForServiceHost(serviceInfo, hostParams, osFamily);
    String packageList = gson.toJson(packages);
    hostParams.put(PACKAGE_LIST, packageList);

    Set<String> userSet = configHelper.getPropertyValuesWithPropertyType(stackId, PropertyType.USER, cluster);
    String userList = gson.toJson(userSet);
    hostParams.put(USER_LIST, userList);

    Set<String> groupSet = configHelper.getPropertyValuesWithPropertyType(stackId, PropertyType.GROUP, cluster);
    String groupList = gson.toJson(groupSet);
    hostParams.put(GROUP_LIST, groupList);

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
    execCmd.setRoleParams(roleParams);
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

  private RequestStageContainer doStageCreation(RequestStageContainer requestStages,
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
      LOG.debug("Created 0 stages");
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
      Map<String, Set<String>> clusterHostInfo = StageUtils.getClusterHostInfo(
          clusters.getHostsForCluster(cluster.getClusterName()), cluster);

      String clusterHostInfoJson = StageUtils.getGson().toJson(clusterHostInfo);
      String HostParamsJson = StageUtils.getGson().toJson(
          customCommandExecutionHelper.createDefaultHostParams(cluster));

      Stage stage = createNewStage(requestStages.getLastStageId(), cluster,
          requestStages.getId(), requestProperties.get(REQUEST_CONTEXT_PROPERTY),
          clusterHostInfoJson, "{}", HostParamsJson);

      Collection<ServiceComponentHost> componentsToEnableKerberos = new ArrayList<ServiceComponentHost>();
      Set<String> hostsToForceKerberosOperations = new HashSet<String>();

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

                  // If the state is transitioning from INIT TO INSTALLED and the cluster has Kerberos
                  // enabled, mark this ServiceComponentHost to see if anything needs to be done to
                  // make sure it is properly configured.  The Kerberos-related stages needs to be
                  // between the INSTALLED and STARTED states because some services need to set up
                  // the host (i,e, create user accounts, etc...) before Kerberos-related tasks an
                  // occur (like distribute keytabs)
                  if((oldSchState == State.INIT) && kerberosHelper.isClusterKerberosEnabled(cluster)) {
                    try {
                      kerberosHelper.configureService(cluster, scHost);
                    } catch (KerberosInvalidConfigurationException e) {
                      throw new AmbariException(e.getMessage(), e);
                    }

                    componentsToEnableKerberos.add(scHost);

                    if(Service.Type.KERBEROS.name().equalsIgnoreCase(scHost.getServiceName()) &&
                        Role.KERBEROS_CLIENT.name().equalsIgnoreCase(scHost.getServiceComponentName())) {
                      // Since the KERBEROS/KERBEROS_CLIENT is about to be moved from the INIT to the
                      // INSTALLED state (and it should be by the time the stages (in this request)
                      // that need to be execute), collect the relevant hostname to make sure the
                      // Kerberos logic doest not skip operations for it.
                      hostsToForceKerberosOperations.add(scHost.getHostName());
                    }

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
            Map<String, Map<String, Map<String, String>>> configurationAttributes = new TreeMap<String, Map<String, Map<String, String>>>();
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

            createHostAction(cluster, stage, scHost, configurations, configurationAttributes, configTags,
              roleCommand, requestParameters, event);
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

        customCommandExecutionHelper.addServiceCheckAction(stage, clientHost,
          smokeTestRole, nowTimestamp, serviceName,
          componentName, null, false);
      }

      RoleCommandOrder rco = getRoleCommandOrder(cluster);
      RoleGraph rg = new RoleGraph(rco);

      rg.build(stage);
      requestStages.addStages(rg.getStages());

      if (!componentsToEnableKerberos.isEmpty()) {
        Map<String, Collection<String>> serviceFilter = new HashMap<String, Collection<String>>();

        for (ServiceComponentHost scHost : componentsToEnableKerberos) {
          String serviceName = scHost.getServiceName();
          Collection<String> componentFilter = serviceFilter.get(serviceName);

          if (componentFilter == null) {
            componentFilter = new HashSet<String>();
            serviceFilter.put(serviceName, componentFilter);
          }

          componentFilter.add(scHost.getServiceComponentName());
        }

        try {
          kerberosHelper.ensureIdentities(cluster, serviceFilter, null, hostsToForceKerberosOperations, requestStages);
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

    requestStages = doStageCreation(requestStages, cluster, changedServices, changedComponents,
        changedHosts, requestParameters, requestProperties,
        runSmokeTest, reconfigureClients);

    updateServiceStates(changedServices, changedComponents, changedHosts, ignoredHosts);
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

  @Override
  public synchronized void updateUsers(Set<UserRequest> requests) throws AmbariException {
    for (UserRequest request : requests) {
      User u = users.getAnyUser(request.getUsername());
      if (null == u) {
        continue;
      }

      if (null != request.getOldPassword() && null != request.getPassword()) {
        users.modifyPassword(u.getUserName(), request.getOldPassword(),
            request.getPassword());
      }

      if (null != request.isActive()) {
        users.setUserActive(u.getUserName(), request.isActive());
      }

      if (null != request.isAdmin()) {
        if (request.isAdmin()) {
          users.grantAdminPrivilege(u.getUserId());
        } else {
          users.revokeAdminPrivilege(u.getUserId());
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

      setRestartRequiredServices(service, request.getHostname());

      if (!safeToRemoveSCHs.containsKey(component)) {
        safeToRemoveSCHs.put(component, new HashSet<ServiceComponentHost>());
      }
      safeToRemoveSCHs.get(component).add(componentHost);
    }

    for (Entry<ServiceComponent, Set<ServiceComponentHost>> entry
            : safeToRemoveSCHs.entrySet()) {
      for (ServiceComponentHost componentHost : entry.getValue()) {
        String included_hostname = componentHost.getHostName();
        String serviceName = entry.getKey().getServiceName();
        String master_component_name = null;
        String slave_component_name = componentHost.getServiceComponentName();
        HostComponentAdminState desiredAdminState = componentHost.getComponentAdminState();
        State slaveState = componentHost.getState();
        //Delete hostcomponents
        entry.getKey().deleteServiceComponentHosts(componentHost.getHostName());
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
                    entry.getKey().getClusterName(), AmbariCustomCommandExecutionHelper.DECOMMISSION_COMMAND_NAME, null,
                    Collections.singletonList(resourceFilter), null, params, true);
            //Send request
            createAction(actionRequest, requestProperties);
          }

          //Mark master component as needed to restart for remove host info from components UI
          Cluster cluster = clusters.getCluster(entry.getKey().getClusterName());
          Service service = cluster.getService(serviceName);
          ServiceComponent sc = service.getServiceComponent(master_component_name);

          if (sc != null && sc.isMasterComponent()) {
            for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
              sch.setRestartRequired(true);
            }
          }

        }

      }
    }

    // set restartRequired flag for  monitoring services
    if (!safeToRemoveSCHs.isEmpty()) {
      setMonitoringServicesRestartRequired(requests);
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

    for (ServiceConfigVersionResponse response : cluster.getServiceConfigVersions()) {
      if (request.getServiceName() != null && !StringUtils.equals(request.getServiceName(), response.getServiceName())) {
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
          UserResponse resp = new UserResponse(u.getUserName(), u.isLdapUser(), u.isActive(), u.isAdmin());
          resp.setGroups(new HashSet<String>(u.getGroups()));
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
          UserResponse resp = new UserResponse(u.getUserName(), u.isLdapUser(), u.isActive(), u.isAdmin());
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
          final GroupResponse response = new GroupResponse(group.getGroupName(), group.isLdapGroup());
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
          final GroupResponse response = new GroupResponse(group.getGroupName(), group.isLdapGroup());
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

    long requestId = actionManager.getNextRequestId();
    RequestStageContainer requestStageContainer = new RequestStageContainer(
        requestId,
        null,
        requestFactory,
        actionManager,
        actionRequest);

    ExecuteCommandJson jsons = customCommandExecutionHelper.getCommandJson(actionExecContext, cluster);
    String commandParamsForStage = jsons.getCommandParamsForStage();

    // If the request is to perform the Kerberos service check, set up the stages to
    // ensure that the (cluster-level) smoke user principal and keytab is available on all hosts
    boolean kerberosServiceCheck = Role.KERBEROS_SERVICE_CHECK.name().equals(actionRequest.getCommandName());
    if (kerberosServiceCheck) {
      // Parse the command parameters into a map so that additional values may be added to it
      Map<String, String> commandParamsStage = gson.fromJson(commandParamsForStage,
          new TypeToken<Map<String, String>>() {
          }.getType());

      try {
        requestStageContainer = kerberosHelper.createTestIdentity(cluster, commandParamsStage, requestStageContainer);
      } catch (KerberosOperationException e) {
        throw new IllegalArgumentException(e.getMessage(), e);
      }

      // Recreate commandParamsForStage with the added values
      commandParamsForStage = gson.toJson(commandParamsStage);
    }

    Stage stage = createNewStage(requestStageContainer.getLastStageId(), cluster, requestId, requestContext,
        jsons.getClusterHostInfo(), commandParamsForStage, jsons.getHostParamsForStage());

    if (actionRequest.isCommand()) {
      customCommandExecutionHelper.addExecutionCommandsToStage(actionExecContext, stage, requestProperties, false);
    } else {
      actionExecutionHelper.addExecutionCommandsToStage(actionExecContext, stage, false);
    }

    RoleGraph rg;
    if (null != cluster) {
      RoleCommandOrder rco = getRoleCommandOrder(cluster);
      rg = new RoleGraph(rco);
    } else {
      rg = new RoleGraph();
    }

    rg.build(stage);
    List<Stage> stages = rg.getStages();

    if (stages != null && !stages.isEmpty()) {
      // If this is a Kerberos service check, set the service check stage(s) to be skip-able so that
      // the clean up stages will still be triggered in the event of a failure.
      if (kerberosServiceCheck) {
        for (Stage s : stages) {
          s.setSkippable(true);
        }
      }

      requestStageContainer.addStages(stages);
    }

    // If the request is to perform the Kerberos service check, delete the test-specific principal
    // and keytab that was created for this service check
    if (kerberosServiceCheck) {
      // Parse the command parameters into a map so that existing values may be accessed and
      // additional values may be added to it.
      Map<String, String> commandParamsStage = gson.fromJson(commandParamsForStage,
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

    Set<RepositoryResponse> responses = new HashSet<RepositoryResponse>();

    if (repositoryVersionId != null) {
      final RepositoryVersionEntity repositoryVersion = repositoryVersionDAO.findByPK(repositoryVersionId);
      if (repositoryVersion != null) {
        for (OperatingSystemEntity operatingSystem: repositoryVersion.getOperatingSystems()) {
          if (operatingSystem.getOsType().equals(osType)) {
            for (RepositoryEntity repository: operatingSystem.getRepositories()) {
              final RepositoryResponse response = new RepositoryResponse(repository.getBaseUrl(), osType, repository.getRepositoryId(), repository.getName(), repository.getRepoComponents(), "", "", "");
              response.setRepositoryVersionId(repositoryVersionId);
              response.setStackName(repositoryVersion.getStackName());
              response.setStackVersion(repositoryVersion.getStackVersion());
              responses.add(response);
            }
            break;
          }
        }
      }
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

      if (null != rr.getBaseUrl()) {
        if (rr.isVerifyBaseUrl()) {
          verifyRepository(rr);
        }
        if (rr.getRepositoryVersionId() != null) {
          throw new AmbariException("Can't directly update repositories in repository_version, update the repository_version instead");
        }
        ambariMetaInfo.updateRepoBaseURL(rr.getStackName(), rr.getStackVersion(), rr.getOsType(), rr.getRepoId(), rr.getBaseUrl());
      }
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

    RepositoryInfo repositoryInfo = ambariMetaInfo.getRepository(request.getStackName(), request.getStackVersion(), request.getOsType(), request.getRepoId());
    String repoName = repositoryInfo.getRepoName();

    String errorMessage = null;

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
          break;
        }

      }else{
        try {
          IOUtils.readLines(usp.readFrom(spec));
        } catch (IOException ioe) {
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

    if (errorMessage != null) {
      LOG.error(errorMessage);
      throw new IllegalArgumentException(errorMessage);
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

    if (repositoryVersionId != null) {
      final RepositoryVersionEntity repositoryVersion = repositoryVersionDAO.findByPK(repositoryVersionId);
      if (repositoryVersion != null) {
        for (OperatingSystemEntity operatingSystem: repositoryVersion.getOperatingSystems()) {
          final OperatingSystemResponse response = new OperatingSystemResponse(operatingSystem.getOsType());
          response.setRepositoryVersionId(repositoryVersionId);
          response.setStackName(repositoryVersion.getStackName());
          response.setStackVersion(repositoryVersion.getStackVersion());
          responses.add(response);
        }
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
}
