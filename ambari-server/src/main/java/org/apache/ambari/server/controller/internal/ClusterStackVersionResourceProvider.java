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
package org.apache.ambari.server.controller.internal;

import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.JDK_LOCATION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.ActionExecutionContext;
import org.apache.ambari.server.controller.AmbariActionExecutionHelper;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterVersionDAO;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.OperatingSystemEntity;
import org.apache.ambari.server.orm.entities.RepositoryEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.serveraction.upgrades.FinalizeUpgradeAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.ServiceOsSpecific;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.utils.StageUtils;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;

/**
 * Resource provider for cluster stack versions resources.
 */
@StaticallyInject
public class ClusterStackVersionResourceProvider extends AbstractControllerResourceProvider {

  // ----- Property ID constants ---------------------------------------------

  protected static final String CLUSTER_STACK_VERSION_ID_PROPERTY_ID                   = PropertyHelper.getPropertyId("ClusterStackVersions", "id");
  protected static final String CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID         = PropertyHelper.getPropertyId("ClusterStackVersions", "cluster_name");
  protected static final String CLUSTER_STACK_VERSION_STACK_PROPERTY_ID                = PropertyHelper.getPropertyId("ClusterStackVersions", "stack");
  protected static final String CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID              = PropertyHelper.getPropertyId("ClusterStackVersions", "version");
  protected static final String CLUSTER_STACK_VERSION_STATE_PROPERTY_ID                = PropertyHelper.getPropertyId("ClusterStackVersions", "state");
  protected static final String CLUSTER_STACK_VERSION_HOST_STATES_PROPERTY_ID          = PropertyHelper.getPropertyId("ClusterStackVersions", "host_states");
  protected static final String CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID   = PropertyHelper.getPropertyId("ClusterStackVersions", "repository_version");

  protected static final String INSTALL_PACKAGES_ACTION = "install_packages";
  protected static final String INSTALL_PACKAGES_FULL_NAME = "Install version";

  @SuppressWarnings("serial")
  private static Set<String> pkPropertyIds = new HashSet<String>() {
    {
      add(CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID);
      add(CLUSTER_STACK_VERSION_ID_PROPERTY_ID);
      add(CLUSTER_STACK_VERSION_STACK_PROPERTY_ID);
      add(CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID);
      add(CLUSTER_STACK_VERSION_STATE_PROPERTY_ID);
      add(CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID);
    }
  };

  @SuppressWarnings("serial")
  private static Set<String> propertyIds = new HashSet<String>() {
    {
      add(CLUSTER_STACK_VERSION_ID_PROPERTY_ID);
      add(CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID);
      add(CLUSTER_STACK_VERSION_STACK_PROPERTY_ID);
      add(CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID);
      add(CLUSTER_STACK_VERSION_HOST_STATES_PROPERTY_ID);
      add(CLUSTER_STACK_VERSION_STATE_PROPERTY_ID);
      add(CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID);
    }
  };

  @SuppressWarnings("serial")
  private static Map<Type, String> keyPropertyIds = new HashMap<Type, String>() {
    {
      put(Type.Cluster, CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID);
      put(Type.ClusterStackVersion, CLUSTER_STACK_VERSION_ID_PROPERTY_ID);
      put(Type.Stack, CLUSTER_STACK_VERSION_STACK_PROPERTY_ID);
      put(Type.StackVersion, CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID);
      put(Type.RepositoryVersion, CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID);
    }
  };

  @Inject
  private static ClusterVersionDAO clusterVersionDAO;

  @Inject
  private static HostVersionDAO hostVersionDAO;

  @Inject
  private static RepositoryVersionDAO repositoryVersionDAO;

  @Inject
  private static HostRoleCommandFactory hostRoleCommandFactory;

  private static Gson gson = StageUtils.getGson();

  @Inject
  private static Provider<AmbariActionExecutionHelper> actionExecutionHelper;

  @Inject
  private static StageFactory stageFactory;

  @Inject
  private static ClusterDAO clusterDAO;

  @Inject
  private static RequestFactory requestFactory;

  @Inject
  private static Configuration configuration;

  @Inject
  private static Injector injector;

  /**
   * We have to include such a hack here, because if we
   * make finalizeUpgradeAction field static and request injection
   * for it, there will be a circle dependency error
   */
  private FinalizeUpgradeAction finalizeUpgradeAction = injector.getInstance(FinalizeUpgradeAction.class);

  /**
   * Constructor.
   */
  public ClusterStackVersionResourceProvider(
          AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate) throws
      SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<Resource> resources = new HashSet<Resource>();
    final Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    final Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);

    List<ClusterVersionEntity> requestedEntities = new ArrayList<ClusterVersionEntity>();
    for (Map<String, Object> propertyMap: propertyMaps) {
      final String clusterName = propertyMap.get(CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID).toString();
      final Long id;
      if (propertyMap.get(CLUSTER_STACK_VERSION_ID_PROPERTY_ID) == null && propertyMaps.size() == 1) {
        requestedEntities = clusterVersionDAO.findByCluster(clusterName);
      } else {
        try {
          id = Long.parseLong(propertyMap.get(CLUSTER_STACK_VERSION_ID_PROPERTY_ID).toString());
        } catch (Exception ex) {
          throw new SystemException("Stack version should have numerical id");
        }
        final ClusterVersionEntity entity = clusterVersionDAO.findByPK(id);
        if (entity == null) {
          throw new NoSuchResourceException("There is no stack version with id " + id);
        } else {
          requestedEntities.add(entity);
        }
      }
    }

    for (ClusterVersionEntity entity: requestedEntities) {
      final Resource resource = new ResourceImpl(Resource.Type.ClusterStackVersion);

      final Map<String, List<String>> hostStates = new HashMap<String, List<String>>();
      for (RepositoryVersionState state: RepositoryVersionState.values()) {
        hostStates.put(state.name(), new ArrayList<String>());
      }

      StackEntity repoVersionStackEntity = entity.getRepositoryVersion().getStack();
      StackId repoVersionStackId = new StackId(repoVersionStackEntity);

      for (HostVersionEntity hostVersionEntity : hostVersionDAO.findByClusterStackAndVersion(
          entity.getClusterEntity().getClusterName(), repoVersionStackId,
          entity.getRepositoryVersion().getVersion())) {
        hostStates.get(hostVersionEntity.getState().name()).add(hostVersionEntity.getHostName());
      }
      StackId stackId = new StackId(entity.getRepositoryVersion().getStack());
      RepositoryVersionEntity repoVerEntity = repositoryVersionDAO.findByStackAndVersion(
          stackId, entity.getRepositoryVersion().getVersion());

      setResourceProperty(resource, CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID, entity.getClusterEntity().getClusterName(), requestedIds);
      setResourceProperty(resource, CLUSTER_STACK_VERSION_HOST_STATES_PROPERTY_ID, hostStates, requestedIds);
      setResourceProperty(resource, CLUSTER_STACK_VERSION_ID_PROPERTY_ID, entity.getId(), requestedIds);
      setResourceProperty(resource, CLUSTER_STACK_VERSION_STACK_PROPERTY_ID, stackId.getStackName(), requestedIds);
      setResourceProperty(resource, CLUSTER_STACK_VERSION_STATE_PROPERTY_ID, entity.getState().name(), requestedIds);
      setResourceProperty(resource, CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID, stackId.getStackVersion(), requestedIds);
      if (repoVerEntity!=null) {
        Long repoVersionId = repoVerEntity.getId();
        setResourceProperty(resource, CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID, repoVersionId, requestedIds);
      }

      if (predicate == null || predicate.evaluate(resource)) {
        resources.add(resource);
      }
    }
    return resources;
  }


  @Override
  public RequestStatus createResources(Request request) throws SystemException,
          UnsupportedPropertyException, ResourceAlreadyExistsException,
          NoSuchParentResourceException {
    Iterator<Map<String, Object>> iterator = request.getProperties().iterator();
    String clName;
    final String desiredRepoVersion;
    String stackName;
    String stackVersion;
    if (request.getProperties().size() != 1) {
      throw new UnsupportedOperationException("Multiple requests cannot be executed at the same time.");
    }
    Map<String, Object> propertyMap = iterator.next();

    Set<String> requiredProperties = new HashSet<String>(){{
      add(CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID);
      add(CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID);
      add(CLUSTER_STACK_VERSION_STACK_PROPERTY_ID);
      add(CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID);
    }};

    for (String requiredProperty : requiredProperties) {
      if (! propertyMap.containsKey(requiredProperty)) {
        throw new IllegalArgumentException(
                String.format("The required property %s is not defined",
                        requiredProperty));
      }
    }

    clName = (String) propertyMap.get(CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID);
    desiredRepoVersion = (String) propertyMap.get(CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID);

    Cluster cluster;
    Map<String, Host> hostsForCluster;

    AmbariManagementController managementController = getManagementController();
    AmbariMetaInfo ami = managementController.getAmbariMetaInfo();
    try {
      cluster = managementController.getClusters().getCluster(clName);
      hostsForCluster = managementController.getClusters().getHostsForCluster(clName);
    } catch (AmbariException e) {
      throw new NoSuchParentResourceException(e.getMessage(), e);
    }

    final StackId stackId;
    if (propertyMap.containsKey(CLUSTER_STACK_VERSION_STACK_PROPERTY_ID) &&
            propertyMap.containsKey(CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID)) {
      stackName = (String) propertyMap.get(CLUSTER_STACK_VERSION_STACK_PROPERTY_ID);
      stackVersion = (String) propertyMap.get(CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID);
      stackId = new StackId(stackName, stackVersion);
      if (! ami.isSupportedStack(stackName, stackVersion)) {
        throw new NoSuchParentResourceException(String.format("Stack %s is not supported",
                stackId));
      }
    } else { // Using stack that is current for cluster
      StackId currentStackVersion = cluster.getCurrentStackVersion();
      stackName = currentStackVersion.getStackName();
      stackVersion = currentStackVersion.getStackVersion();
      stackId = currentStackVersion;
    }

    RepositoryVersionEntity repoVersionEnt = repositoryVersionDAO.findByStackAndVersion(
        stackId, desiredRepoVersion);
    if (repoVersionEnt == null) {
      throw new IllegalArgumentException(String.format(
              "Repo version %s is not available for stack %s",
              desiredRepoVersion, stackId));
    }
    List<OperatingSystemEntity> operatingSystems = repoVersionEnt.getOperatingSystems();
    Map<String, List<RepositoryEntity>> perOsRepos = new HashMap<String, List<RepositoryEntity>>();
    for (OperatingSystemEntity operatingSystem : operatingSystems) {
      perOsRepos.put(operatingSystem.getOsType(), operatingSystem.getRepositories());
    }

    RequestStageContainer req = createRequest();

    Iterator<Host> hostsForClusterIter = hostsForCluster.values().iterator();
    Map<String, String> hostLevelParams = new HashMap<String, String>();
    hostLevelParams.put(JDK_LOCATION, getManagementController().getJdkResourceUrl());
    String hostParamsJson = StageUtils.getGson().toJson(hostLevelParams);

    int maxTasks = configuration.getAgentPackageParallelCommandsLimit();
    int hostCount = hostsForCluster.size();
    int batchCount = (int) (Math.ceil((double)hostCount / maxTasks));

    long stageId = req.getLastStageId() + 1;
    if (0L == stageId) {
      stageId = 1L;
    }

    ArrayList<Stage> stages = new ArrayList<Stage>(batchCount);
    for (int batchId = 1; batchId <= batchCount ; batchId++) {
      // Create next stage
      String stageName;
      if (batchCount > 1) {
        stageName = INSTALL_PACKAGES_FULL_NAME;
      } else {
        stageName = String.format(INSTALL_PACKAGES_FULL_NAME +
                ". Batch %d of %d", batchId, batchCount);
      }
      Stage stage = stageFactory.createNew(req.getId(),
              "/tmp/ambari",
              cluster.getClusterName(),
              cluster.getClusterId(),
              stageName,
              "{}",
              "{}",
              hostParamsJson
      );
      stage.setStageId(stageId);
      stages.add(stage);
      stageId++;
      // Populate with commands for host
      for (int i = 0; i < maxTasks && hostsForClusterIter.hasNext(); i++) {
        Host host = hostsForClusterIter.next();
        addHostVersionInstallCommandsToStage(desiredRepoVersion,
                cluster, managementController, ami, stackId, perOsRepos, stage, host);
      }
    }
    req.addStages(stages);

    try {
      ClusterVersionEntity existingCSVer = clusterVersionDAO.findByClusterAndStackAndVersion(
          clName, stackId, desiredRepoVersion);
      if (existingCSVer == null) {
        try {
          // Create/persist new cluster stack version
          cluster.createClusterVersion(stackId,
              desiredRepoVersion, managementController.getAuthName(),
              RepositoryVersionState.INSTALLING);
          existingCSVer = clusterVersionDAO.findByClusterAndStackAndVersion(
              clName, stackId, desiredRepoVersion);
        } catch (AmbariException e) {
          throw new SystemException(
                  String.format(
                          "Can not create cluster stack version %s for cluster %s",
              desiredRepoVersion, clName), e);
        }
      } else {
        // Move CSV into INSTALLING state (retry installation)
        cluster.transitionClusterVersion(stackId,
            desiredRepoVersion, RepositoryVersionState.INSTALLING);
      }

      // Will also initialize all Host Versions in an INSTALLING state.
      cluster.inferHostVersions(existingCSVer);

      req.persist();

    } catch (AmbariException e) {
      throw new SystemException("Can not persist request", e);
    }
    return getRequestStatus(req.getRequestStatusResponse());
  }

  private void addHostVersionInstallCommandsToStage(final String desiredRepoVersion,
                                                    Cluster cluster, AmbariManagementController managementController,
                                                    AmbariMetaInfo ami,
                                                    final StackId stackId,
                                                    Map<String, List<RepositoryEntity>> perOsRepos,
                                                    Stage stage, Host host) throws SystemException {
    // Determine repositories for host
    final List<RepositoryEntity> repoInfo = perOsRepos.get(host.getOsFamily());
    if (repoInfo == null) {
      throw new SystemException(String.format("Repositories for os type %s are " +
                      "not defined. Repo version=%s, stackId=%s",
              host.getOsFamily(), desiredRepoVersion, stackId));
    }
    // For every host at cluster, determine packages for all installed services
    List<ServiceOsSpecific.Package> packages = new ArrayList<ServiceOsSpecific.Package>();
    Set<String> servicesOnHost = new HashSet<String>();
    List<ServiceComponentHost> components = cluster.getServiceComponentHosts(host.getHostName());
    for (ServiceComponentHost component : components) {
      servicesOnHost.add(component.getServiceName());
    }

    for (String serviceName : servicesOnHost) {
      ServiceInfo info;
      try {
        info = ami.getService(stackId.getStackName(), stackId.getStackVersion(), serviceName);
      } catch (AmbariException e) {
        throw new SystemException("Cannot enumerate services", e);
      }

      List<ServiceOsSpecific.Package> packagesForService = managementController.getPackagesForServiceHost(info,
              new HashMap<String, String>(), // Contents are ignored
              host.getOsFamily());
      for (ServiceOsSpecific.Package aPackage : packagesForService) {
        if (! aPackage.getSkipUpgrade()) {
          packages.add(aPackage);
        }
      }
    }
    final String packageList = gson.toJson(packages);
    final String repoList = gson.toJson(repoInfo);

    Map<String, String> params = new HashMap<String, String>() {{
      put("stack_id", stackId.getStackId());
      put("repository_version", desiredRepoVersion);
      put("base_urls", repoList);
      put("package_list", packageList);
    }};

    // add host to this stage
    RequestResourceFilter filter = new RequestResourceFilter(null, null,
            Collections.singletonList(host.getHostName()));

    ActionExecutionContext actionContext = new ActionExecutionContext(
            cluster.getClusterName(), INSTALL_PACKAGES_ACTION,
            Collections.singletonList(filter),
            params);
    actionContext.setTimeout(Short.valueOf(configuration.getDefaultAgentTaskTimeout(true)));

    try {
      actionExecutionHelper.get().addExecutionCommandsToStage(actionContext, stage, false);
    } catch (AmbariException e) {
      throw new SystemException("Can not modify stage", e);
    }
  }


  private RequestStageContainer createRequest() {
    ActionManager actionManager = getManagementController().getActionManager();

    RequestStageContainer requestStages = new RequestStageContainer(
            actionManager.getNextRequestId(), null, requestFactory, actionManager);
    requestStages.setRequestContext(String.format(INSTALL_PACKAGES_FULL_NAME));

    return requestStages;
  }

  /**
   * The only appliance of this method is triggering Finalize during
   * manual Rolling Upgrade
   */
  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
    try {
      Iterator<Map<String, Object>> iterator = request.getProperties().iterator();
      String clName;
      final String desiredRepoVersion;
      if (request.getProperties().size() != 1) {
        throw new UnsupportedOperationException("Multiple requests cannot be executed at the same time.");
      }
      Map<String, Object> propertyMap = iterator.next();

      Set<String> requiredProperties = new HashSet<String>() {{
        add(CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID);
        add(CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID);
        add(CLUSTER_STACK_VERSION_STATE_PROPERTY_ID);
      }};

      for (String requiredProperty : requiredProperties) {
        if (!propertyMap.containsKey(requiredProperty)) {
          throw new IllegalArgumentException(
                  String.format("The required property %s is not defined",
                          requiredProperty));
        }
      }

      clName = (String) propertyMap.get(CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID);
      String desiredDisplayRepoVersion = (String) propertyMap.get(CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID);
      RepositoryVersionEntity rve = repositoryVersionDAO.findByDisplayName(desiredDisplayRepoVersion);
      if (rve == null) {
        throw new IllegalArgumentException(
                  String.format("Repository version with display name %s does not exist",
                          desiredDisplayRepoVersion));
      }
      desiredRepoVersion = rve.getVersion();
      String newStateStr = (String) propertyMap.get(CLUSTER_STACK_VERSION_STATE_PROPERTY_ID);

      LOG.info("Initiating finalization for manual upgrade to version {} for cluster {}",
              desiredRepoVersion, clName);

      Map<String, String> args = new HashMap<String, String>();
      if (newStateStr.equals(RepositoryVersionState.CURRENT.toString())) {
        // Finalize upgrade workflow
        args.put(FinalizeUpgradeAction.UPGRADE_DIRECTION_KEY, "upgrade");
      } else if (newStateStr.equals(RepositoryVersionState.INSTALLED.toString())) {
        // Finalize downgrade workflow
        args.put(FinalizeUpgradeAction.UPGRADE_DIRECTION_KEY, "downgrade");
      } else {
        throw new IllegalArgumentException(
          String.format("Invalid desired state %s. Should be either CURRENT or INSTALLED",
                  newStateStr));
      }

      // Get a host name to populate the hostrolecommand table's hostEntity.
      ClusterEntity cluster = clusterDAO.findByName(clName);
      String defaultHostName = null;
      List<HostEntity> hosts = new ArrayList(cluster.getHostEntities());
      if (hosts != null && !hosts.isEmpty()) {
        Collections.sort(hosts);
        defaultHostName = hosts.get(0).getHostName();
      }
      if (defaultHostName == null) {
        throw new AmbariException("Could not find at least one host to set the command for");
      }

      args.put(FinalizeUpgradeAction.VERSION_KEY, desiredRepoVersion);
      args.put(FinalizeUpgradeAction.CLUSTER_NAME_KEY, clName);

      ExecutionCommand command = new ExecutionCommand();
      command.setCommandParams(args);
      command.setClusterName(clName);
      finalizeUpgradeAction.setExecutionCommand(command);
      
      HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(defaultHostName,
              Role.AMBARI_SERVER_ACTION, null, null);
      finalizeUpgradeAction.setHostRoleCommand(hostRoleCommand);

      CommandReport report = finalizeUpgradeAction.execute(null);

      LOG.info("Finalize output:");
      LOG.info("STDOUT: {}", report.getStdOut());
      LOG.info("STDERR: {}", report.getStdErr());

      if (report.getStatus().equals(HostRoleStatus.COMPLETED.toString())) {
        return getRequestStatus(null);
      } else {
        String detailedOutput = "Finalization failed. More details: \n" +
                "STDOUT: " + report.getStdOut() + "\n" +
                "STDERR: " + report.getStdErr();
        throw new SystemException(detailedOutput);
      }
    } catch (AmbariException e) {
      throw new SystemException("Can not perform request", e);
    } catch (InterruptedException e) {
      throw new SystemException("Can not perform request", e);
    }
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
    throw new SystemException("Method not supported");
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }
}
