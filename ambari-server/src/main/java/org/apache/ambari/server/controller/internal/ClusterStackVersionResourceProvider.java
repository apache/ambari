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
import java.util.EnumSet;
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
import org.apache.ambari.server.agent.ExecutionCommand.KeyNames;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.ActionExecutionContext;
import org.apache.ambari.server.controller.AmbariActionExecutionHelper;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.RequestStatusResponse;
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
import org.apache.ambari.server.orm.dao.ClusterVersionDAO;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.OperatingSystemEntity;
import org.apache.ambari.server.orm.entities.RepositoryEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.serveraction.upgrades.FinalizeUpgradeAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.ServiceOsSpecific;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.ambari.server.utils.VersionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;

/**
 * Resource provider for cluster stack versions resources.
 */
@StaticallyInject
public class ClusterStackVersionResourceProvider extends AbstractControllerResourceProvider {

  // ----- Property ID constants ---------------------------------------------

  protected static final String CLUSTER_STACK_VERSION_ID_PROPERTY_ID = PropertyHelper.getPropertyId("ClusterStackVersions", "id");
  protected static final String CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("ClusterStackVersions", "cluster_name");
  protected static final String CLUSTER_STACK_VERSION_STACK_PROPERTY_ID = PropertyHelper.getPropertyId("ClusterStackVersions", "stack");
  protected static final String CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID = PropertyHelper.getPropertyId("ClusterStackVersions", "version");
  protected static final String CLUSTER_STACK_VERSION_STATE_PROPERTY_ID = PropertyHelper.getPropertyId("ClusterStackVersions", "state");
  protected static final String CLUSTER_STACK_VERSION_HOST_STATES_PROPERTY_ID = PropertyHelper.getPropertyId("ClusterStackVersions", "host_states");
  protected static final String CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID  = PropertyHelper.getPropertyId("ClusterStackVersions", "repository_version");
  protected static final String CLUSTER_STACK_VERSION_STAGE_SUCCESS_FACTOR  = PropertyHelper.getPropertyId("ClusterStackVersions", "success_factor");

  /**
   * Forces the {@link HostVersionEntity}s to a specific
   * {@link RepositoryVersionState}. When used during the creation of
   * {@link HostVersionEntity}s, this will set the state to
   * {@link RepositoryVersionState#INSTALLED}. When used during the update of a
   * cluster stack version, this will force all entities to
   * {@link RepositoryVersionState#CURRENT}.
   *
   */
  protected static final String CLUSTER_STACK_VERSION_FORCE = "ClusterStackVersions/force";

  protected static final String INSTALL_PACKAGES_ACTION = "install_packages";
  protected static final String INSTALL_PACKAGES_FULL_NAME = "Install version";

  /**
   * The default success factor that will be used when determining if a stage's
   * failure should cause other stages to abort. Consider a scenario with 1000
   * hosts, broken up into 10 stages. Each stage would have 100 hosts. If the
   * success factor was 100%, then any failure in stage 1 woudl cause all 9
   * other stages to abort. If set to 90%, then 10 hosts would need to fail for
   * the other stages to abort. This is necessary to prevent the abortion of
   * stages based on 1 or 2 errant hosts failing in a large cluster's stack
   * distribution.
   */
  private static final float INSTALL_PACKAGES_SUCCESS_FACTOR = 0.85f;

  private static Set<String> pkPropertyIds = Sets.newHashSet(
      CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID, CLUSTER_STACK_VERSION_ID_PROPERTY_ID,
      CLUSTER_STACK_VERSION_STACK_PROPERTY_ID, CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID,
      CLUSTER_STACK_VERSION_STATE_PROPERTY_ID,
      CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID);

  private static Set<String> propertyIds = Sets.newHashSet(CLUSTER_STACK_VERSION_ID_PROPERTY_ID,
      CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID, CLUSTER_STACK_VERSION_STACK_PROPERTY_ID,
      CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID, CLUSTER_STACK_VERSION_HOST_STATES_PROPERTY_ID,
      CLUSTER_STACK_VERSION_STATE_PROPERTY_ID, CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID,
      CLUSTER_STACK_VERSION_STAGE_SUCCESS_FACTOR, CLUSTER_STACK_VERSION_FORCE);

  private static Map<Type, String> keyPropertyIds = ImmutableMap.<Type, String> builder()
      .put(Type.Cluster, CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID)
      .put(Type.ClusterStackVersion, CLUSTER_STACK_VERSION_ID_PROPERTY_ID)
      .put(Type.Stack, CLUSTER_STACK_VERSION_STACK_PROPERTY_ID)
      .put(Type.StackVersion, CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID)
      .put(Type.RepositoryVersion, CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID)
      .build();

  @Inject
  private static ClusterVersionDAO clusterVersionDAO;

  @Inject
  private static HostVersionDAO hostVersionDAO;

  @Inject
  private static RepositoryVersionDAO repositoryVersionDAO;

  @Inject
  private static HostRoleCommandFactory hostRoleCommandFactory;

  @Inject
  private static Gson gson;

  @Inject
  private static Provider<AmbariActionExecutionHelper> actionExecutionHelper;

  @Inject
  private static StageFactory stageFactory;

  @Inject
  private static RequestFactory requestFactory;

  @Inject
  private static Configuration configuration;

  @Inject
  private static Injector injector;

  @Inject
  private static HostComponentStateDAO hostComponentStateDAO;

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

    setRequiredCreateAuthorizations(EnumSet.of(RoleAuthorization.AMBARI_MANAGE_STACK_VERSIONS, RoleAuthorization.CLUSTER_UPGRADE_DOWNGRADE_STACK));
    setRequiredDeleteAuthorizations(EnumSet.of(RoleAuthorization.AMBARI_MANAGE_STACK_VERSIONS, RoleAuthorization.CLUSTER_UPGRADE_DOWNGRADE_STACK));
    setRequiredUpdateAuthorizations(EnumSet.of(RoleAuthorization.AMBARI_MANAGE_STACK_VERSIONS, RoleAuthorization.CLUSTER_UPGRADE_DOWNGRADE_STACK));
  }

  @Override
  public Set<Resource> getResourcesAuthorized(Request request, Predicate predicate) throws
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
  public RequestStatus createResourcesAuthorized(Request request) throws SystemException,
          UnsupportedPropertyException, ResourceAlreadyExistsException,
          NoSuchParentResourceException {

    if (request.getProperties().size() > 1) {
      throw new UnsupportedOperationException("Multiple requests cannot be executed at the same time.");
    }

    Iterator<Map<String, Object>> iterator = request.getProperties().iterator();

    String clName;
    final String desiredRepoVersion;
    String stackName;
    String stackVersion;

    Map<String, Object> propertyMap = iterator.next();

    Set<String> requiredProperties = new HashSet<String>();
    requiredProperties.add(CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID);
    requiredProperties.add(CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID);
    requiredProperties.add(CLUSTER_STACK_VERSION_STACK_PROPERTY_ID);
    requiredProperties.add(CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID);

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
    AmbariManagementController managementController = getManagementController();
    AmbariMetaInfo ami = managementController.getAmbariMetaInfo();

    try {
      Clusters clusters = managementController.getClusters();
      cluster = clusters.getCluster(clName);
    } catch (AmbariException e) {
      throw new NoSuchParentResourceException(e.getMessage(), e);
    }

    UpgradeEntity entity = cluster.getUpgradeInProgress();
    if (null != entity) {
      throw new IllegalArgumentException(String.format(
          "Cluster %s %s is in progress.  Cannot install packages.",
          cluster.getClusterName(), entity.getDirection().getText(false)));
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
      stackId = currentStackVersion;
    }

    RepositoryVersionEntity repoVersionEnt = repositoryVersionDAO.findByStackAndVersion(
        stackId, desiredRepoVersion);

    if (repoVersionEnt == null) {
      throw new IllegalArgumentException(String.format(
              "Repo version %s is not available for stack %s",
              desiredRepoVersion, stackId));
    }

    VersionDefinitionXml desiredVersionDefinition = null;
    try {
      desiredVersionDefinition = repoVersionEnt.getRepositoryXml();
    } catch (Exception e) {
      throw new IllegalArgumentException(
          String.format("Version %s is backed by a version definition, but it could not be parsed", desiredRepoVersion), e);
    }

    // get all of the host eligible for stack distribution
    List<Host> hosts = getHostsForStackDistribution(cluster);

    /*
    If there is a repository that is already ATTEMPTED to be installed and the version
    is GREATER than the one trying to install, we must fail (until we can support that via Patch Upgrades)

    For example:

    1. Install 2.3.0.0
    2. Register and Install 2.5.0.0 (with or without package-version; it gets computed correctly)
    3. Register 2.4 (without package-version)

    Installation of 2.4 will fail because the way agents invoke installation is to
    install by name.  if the package-version is not known, then the 'newest' is ALWAYS installed.
    In this case, 2.5.0.0.  2.4 is never picked up.
    */
    for (ClusterVersionEntity clusterVersion : clusterVersionDAO.findByCluster(clName)) {
      RepositoryVersionEntity clusterRepoVersion = clusterVersion.getRepositoryVersion();

      int compare = compareVersions(clusterRepoVersion.getVersion(), desiredRepoVersion);

      // ignore earlier versions
      if (compare <= 0) {
        continue;
      }

      // !!! the version is greater to the one to install

      // if the stacks are different, then don't fail (further check same-stack version strings)
      if (!StringUtils.equals(clusterRepoVersion.getStackName(), repoVersionEnt.getStackName())) {
        continue;
      }

      // if there is no backing VDF for the desired version, allow the operation (legacy behavior)
      if (null == desiredVersionDefinition) {
        continue;
      }

      // backing VDF does not define the package version for any of the hosts, cannot install (allows a VDF with package-version)
      for (Host host : hosts) {
        if (StringUtils.isBlank(desiredVersionDefinition.getPackageVersion(host.getOsFamily()))) {
          String msg = String.format("Ambari cannot install version %s.  Version %s is already installed.",
            desiredRepoVersion, clusterRepoVersion.getVersion());
          throw new IllegalArgumentException(msg);
        }
      }
    }

    // if true, then we need to force all new host versions into the INSTALLED state
    boolean forceInstalled = Boolean.parseBoolean((String)propertyMap.get(
        CLUSTER_STACK_VERSION_FORCE));

    final RequestStatusResponse response;

    try {
      if (forceInstalled) {
        createHostVersions(cluster, hosts, stackId, desiredRepoVersion, RepositoryVersionState.INSTALLED);
        response = null;
      } else {
        createHostVersions(cluster, hosts, stackId, desiredRepoVersion,
            RepositoryVersionState.INSTALLING);

        RequestStageContainer installRequest = createOrchestration(cluster, stackId, hosts,
            repoVersionEnt, propertyMap);

        response = installRequest.getRequestStatusResponse();
      }
    } catch (AmbariException e) {
      throw new SystemException("Can not persist request", e);
    }

    return getRequestStatus(response);
  }

  @Transactional
  void createHostVersions(Cluster cluster, List<Host> hosts, StackId stackId,
      String desiredRepoVersion, RepositoryVersionState repoState)
      throws AmbariException, SystemException {
    final String clusterName = cluster.getClusterName();
    final String authName = getManagementController().getAuthName();

    ClusterVersionEntity clusterVersionEntity = clusterVersionDAO.findByClusterAndStackAndVersion(
        clusterName, stackId, desiredRepoVersion);

    if (clusterVersionEntity == null) {
      try {
        // Create/persist new cluster stack version
        cluster.createClusterVersion(stackId, desiredRepoVersion, authName, repoState);

        clusterVersionEntity = clusterVersionDAO.findByClusterAndStackAndVersion(clusterName,
            stackId, desiredRepoVersion);
      } catch (AmbariException e) {
        throw new SystemException(
            String.format("Can not create cluster stack version %s for cluster %s",
                desiredRepoVersion, clusterName), e);
      }
    } else {
      // Move cluster version into the specified state (retry installation)
      cluster.transitionClusterVersion(stackId, desiredRepoVersion, repoState);
    }

    // Will also initialize all Host Versions to the specified state state.
    cluster.transitionHosts(clusterVersionEntity, repoState);

    // Directly transition host versions to NOT_REQUIRED for hosts that don't
    // have versionable components
    for (Host host : hosts) {
      if (!host.hasComponentsAdvertisingVersions(stackId)) {
        transitionHostVersionToNotRequired(host, cluster,
            clusterVersionEntity.getRepositoryVersion());
      }
    }
  }

  @Transactional
  RequestStageContainer createOrchestration(Cluster cluster, StackId stackId,
      List<Host> hosts, RepositoryVersionEntity repoVersionEnt, Map<String, Object> propertyMap)
      throws AmbariException, SystemException {
    final AmbariManagementController managementController = getManagementController();
    final AmbariMetaInfo ami = managementController.getAmbariMetaInfo();

    // build the list of OS repos
    List<OperatingSystemEntity> operatingSystems = repoVersionEnt.getOperatingSystems();
    Map<String, List<RepositoryEntity>> perOsRepos = new HashMap<String, List<RepositoryEntity>>();
    for (OperatingSystemEntity operatingSystem : operatingSystems) {

      if (operatingSystem.isAmbariManagedRepos()) {
        perOsRepos.put(operatingSystem.getOsType(), operatingSystem.getRepositories());
      } else {
        perOsRepos.put(operatingSystem.getOsType(), Collections.<RepositoryEntity> emptyList());
      }
    }

    RequestStageContainer req = createRequest();

    Iterator<Host> hostIterator = hosts.iterator();
    Map<String, String> hostLevelParams = new HashMap<String, String>();
    hostLevelParams.put(JDK_LOCATION, getManagementController().getJdkResourceUrl());
    String hostParamsJson = StageUtils.getGson().toJson(hostLevelParams);

    // Generate cluster host info
    String clusterHostInfoJson;
    try {
      clusterHostInfoJson = StageUtils.getGson().toJson(
        StageUtils.getClusterHostInfo(cluster));
    } catch (AmbariException e) {
      throw new SystemException("Could not build cluster topology", e);
    }

    int maxTasks = configuration.getAgentPackageParallelCommandsLimit();
    int hostCount = hosts.size();
    int batchCount = (int) (Math.ceil((double)hostCount / maxTasks));

    long stageId = req.getLastStageId() + 1;
    if (0L == stageId) {
      stageId = 1L;
    }

    // why does the JSON body parser convert JSON primitives into strings!?
    Float successFactor = INSTALL_PACKAGES_SUCCESS_FACTOR;
    String successFactorProperty = (String) propertyMap.get(CLUSTER_STACK_VERSION_STAGE_SUCCESS_FACTOR);
    if (StringUtils.isNotBlank(successFactorProperty)) {
      successFactor = Float.valueOf(successFactorProperty);
    }

    boolean hasStage = false;

    ArrayList<Stage> stages = new ArrayList<Stage>(batchCount);
    for (int batchId = 1; batchId <= batchCount; batchId++) {
      // Create next stage
      String stageName;
      if (batchCount > 1) {
        stageName = String.format(INSTALL_PACKAGES_FULL_NAME + ". Batch %d of %d", batchId,
            batchCount);
      } else {
        stageName = INSTALL_PACKAGES_FULL_NAME;
      }

      Stage stage = stageFactory.createNew(req.getId(), "/tmp/ambari", cluster.getClusterName(),
          cluster.getClusterId(), stageName, clusterHostInfoJson, "{}", hostParamsJson);

      // if you have 1000 hosts (10 stages with 100 installs), we want to ensure
      // that a single failure doesn't cause all other stages to abort; set the
      // success factor ratio in order to tolerate some failures in a single
      // stage
      stage.getSuccessFactors().put(Role.INSTALL_PACKAGES, successFactor);

      // set and increment stage ID
      stage.setStageId(stageId);
      stageId++;

      // add the stage that was just created
      stages.add(stage);

      // determine services for the repo
      Set<String> serviceNames = new HashSet<>();

      // !!! TODO for patch upgrades, we need to limit the serviceNames to those
      // that are detailed for the repository

      // Populate with commands for host
      for (int i = 0; i < maxTasks && hostIterator.hasNext(); i++) {
        Host host = hostIterator.next();
        if (hostHasVersionableComponents(cluster, serviceNames, ami, stackId, host)) {
          ActionExecutionContext actionContext = getHostVersionInstallCommand(repoVersionEnt,
                  cluster, managementController, ami, stackId, serviceNames, perOsRepos, stage, host);
          if (null != actionContext) {
            try {
              actionExecutionHelper.get().addExecutionCommandsToStage(actionContext, stage, null);
              hasStage = true;
            } catch (AmbariException e) {
              throw new SystemException("Cannot modify stage", e);
            }
          }
        }
      }
    }

    if (!hasStage) {
      throw new SystemException(
          String.format("There are no hosts that have components to install for repository %s",
              repoVersionEnt.getDisplayName()));
    }

    req.addStages(stages);
    req.persist();

    return req;
  }

  private ActionExecutionContext getHostVersionInstallCommand(RepositoryVersionEntity repoVersion,
      Cluster cluster, AmbariManagementController managementController, AmbariMetaInfo ami,
      final StackId stackId, Set<String> repoServices, Map<String, List<RepositoryEntity>> perOsRepos, Stage stage1, Host host)
          throws SystemException {
    // Determine repositories for host
    String osFamily = host.getOsFamily();
    final List<RepositoryEntity> repoInfo = perOsRepos.get(osFamily);
    if (repoInfo == null) {
      throw new SystemException(String.format("Repositories for os type %s are " +
                      "not defined. Repo version=%s, stackId=%s",
        osFamily, repoVersion.getVersion(), stackId));
    }


    // determine packages for all services that are installed on host
    List<ServiceOsSpecific.Package> packages = new ArrayList<ServiceOsSpecific.Package>();
    Set<String> servicesOnHost = new HashSet<String>();
    List<ServiceComponentHost> components = cluster.getServiceComponentHosts(host.getHostName());
    for (ServiceComponentHost component : components) {
      if (repoServices.isEmpty() || repoServices.contains(component.getServiceName())) {
        servicesOnHost.add(component.getServiceName());
      }
    }

    if (servicesOnHost.isEmpty()) {
      return null;
    }
    List<String> blacklistedPackagePrefixes = configuration.getRollingUpgradeSkipPackagesPrefixes();
    for (String serviceName : servicesOnHost) {
      try{
        if(ami.isServiceRemovedInStack(stackId.getStackName(), stackId.getStackVersion(), serviceName)){
          LOG.info(String.format("%s has been removed from stack %s-%s. Skip calculating its installation packages", stackId.getStackName(), stackId.getStackVersion(), serviceName));
          continue; //No need to calculate install packages for removed services
        }
      } catch (AmbariException e1) {
        throw new SystemException(String.format("Cannot obtain stack information for %s-%s", stackId.getStackName(), stackId.getStackVersion()), e1);
      }

      ServiceInfo info;
      try {
        info = ami.getService(stackId.getStackName(), stackId.getStackVersion(), serviceName);
      } catch (AmbariException e) {
        throw new SystemException("Cannot enumerate services", e);
      }

      List<ServiceOsSpecific.Package> packagesForService = managementController.getPackagesForServiceHost(info,
              new HashMap<String, String>(), // Contents are ignored
        osFamily);
      for (ServiceOsSpecific.Package aPackage : packagesForService) {
        if (! aPackage.getSkipUpgrade()) {
          boolean blacklisted = false;
          for(String prefix : blacklistedPackagePrefixes) {
            if (aPackage.getName().startsWith(prefix)) {
              blacklisted = true;
              break;
            }
          }
          if (! blacklisted) {
            packages.add(aPackage);
          }
        }
      }
    }

    final String packageList = gson.toJson(packages);
    final String repoList = gson.toJson(repoInfo);

    Map<String, String> params = new HashMap<String, String>();
    params.put("stack_id", stackId.getStackId());
    params.put("repository_version", repoVersion.getVersion());
    params.put("base_urls", repoList);
    params.put(KeyNames.PACKAGE_LIST, packageList);
    params.put(KeyNames.REPO_VERSION_ID, repoVersion.getId().toString());

    VersionDefinitionXml xml = null;
    try {
      xml = repoVersion.getRepositoryXml();
    } catch (Exception e) {
      throw new SystemException(String.format("Could not load xml from repo version %s",
          repoVersion.getVersion()));
    }

    if (null != xml && StringUtils.isNotBlank(xml.getPackageVersion(osFamily))) {
      params.put(KeyNames.PACKAGE_VERSION, xml.getPackageVersion(osFamily));
    }


    // add host to this stage
    RequestResourceFilter filter = new RequestResourceFilter(null, null,
            Collections.singletonList(host.getHostName()));

    ActionExecutionContext actionContext = new ActionExecutionContext(
            cluster.getClusterName(), INSTALL_PACKAGES_ACTION,
            Collections.singletonList(filter),
            params);
    actionContext.setTimeout(Short.valueOf(configuration.getDefaultAgentTaskTimeout(true)));

    return actionContext;

  }


  /**
   * Returns true if there is at least one versionable component on host for a given
   * stack.
   */
  private boolean hostHasVersionableComponents(Cluster cluster, Set<String> serviceNames, AmbariMetaInfo ami, StackId stackId,
      Host host) throws SystemException {

    List<ServiceComponentHost> components = cluster.getServiceComponentHosts(host.getHostName());

    for (ServiceComponentHost component : components) {
      if (!serviceNames.isEmpty() && !serviceNames.contains(component.getServiceName())) {
        continue;
      }

      ComponentInfo componentInfo;
      try {
        componentInfo = ami.getComponent(stackId.getStackName(),
                stackId.getStackVersion(), component.getServiceName(), component.getServiceComponentName());
      } catch (AmbariException e) {
        // It is possible that the component has been removed from the new stack
        // (example: STORM_REST_API has been removed from HDP-2.2)
        LOG.warn(String.format("Exception while accessing component %s of service %s for stack %s",
            component.getServiceComponentName(), component.getServiceName(), stackId));
        continue;
      }
      if (componentInfo.isVersionAdvertised()) {
        return true;
      }
    }
    return false;
  }


  /**
   *  Sets host versions states to not-required.
   *
   *  Transitioning host version to NOT_REQUIRED state manually is ok since
   *  other completion handlers set success/fail states correctly during heartbeat.
   *  The number of NOT_REQUIRED components for a cluster will be low.
   */
  private void transitionHostVersionToNotRequired(Host host, Cluster cluster, RepositoryVersionEntity repoVersion) {
    LOG.info(String.format("Transitioning version %s on host %s directly to %s" +
                    " without distributing bits to host since it has no versionable components.",
            repoVersion.getVersion(), host.getHostName(), RepositoryVersionState.NOT_REQUIRED));

    for (HostVersionEntity hve : host.getAllHostVersions()) {
      if (hve.getRepositoryVersion().equals(repoVersion)) {
        hve.setState(RepositoryVersionState.NOT_REQUIRED);
        hostVersionDAO.merge(hve);
      }
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
   * manual Stack Upgrade
   */
  @Override
  public RequestStatus updateResourcesAuthorized(Request request, Predicate predicate)
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

      Set<String> requiredProperties = new HashSet<String>();
      requiredProperties.add(CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID);
      requiredProperties.add(CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID);
      requiredProperties.add(CLUSTER_STACK_VERSION_STATE_PROPERTY_ID);

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

      // First, set desired cluster stack version to enable cross-stack upgrade
      StackId stackId = rve.getStackId();
      Cluster cluster = getManagementController().getClusters().getCluster(clName);
      cluster.setDesiredStackVersion(stackId);

      String forceCurrent = (String) propertyMap.get(CLUSTER_STACK_VERSION_FORCE);
      boolean force = false;
      if (null != forceCurrent) {
        force = Boolean.parseBoolean(forceCurrent);
      }

      if (!force) {
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
        String defaultHostName;
        ArrayList<Host> hosts = new ArrayList<Host>(cluster.getHosts());
        if (!hosts.isEmpty()) {
          Collections.sort(hosts);
          defaultHostName = hosts.get(0).getHostName();
        } else {
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
      } else {
        // !!! revisit for PU
        // If forcing to become CURRENT, get the Cluster Version whose state is CURRENT and make sure that
        // the Host Version records for the same Repo Version are also marked as CURRENT.
        ClusterVersionEntity current = cluster.getCurrentClusterVersion();

        if (!current.getRepositoryVersion().equals(rve)) {
          updateVersionStates(current.getClusterId(), current.getRepositoryVersion(), rve);
        }


        return getRequestStatus(null);
      }
    } catch (AmbariException e) {
      throw new SystemException("Cannot perform request", e);
    } catch (InterruptedException e) {
      throw new SystemException("Cannot perform request", e);
    }
  }

  @Override
  public RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
    throw new SystemException("Method not supported");
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  /**
   * Gets all of the hosts in a cluster which are not in "maintenance mode" and
   * are considered to be healthy. In the case of stack distribution, a host
   * must be explicitely marked as being in maintenance mode for it to be
   * considered as unhealthy.
   *
   * @param cluster
   *          the cluster (not {@code null}).
   * @return the list of hosts that are not in maintenance mode and are
   *         elidgable to have a stack distributed to them.
   */
  private List<Host> getHostsForStackDistribution(Cluster cluster) {
    Collection<Host> hosts = cluster.getHosts();
    List<Host> healthyHosts = new ArrayList<>(hosts.size());
    for (Host host : hosts) {
      if (host.getMaintenanceState(cluster.getClusterId()) == MaintenanceState.OFF) {
        healthyHosts.add(host);
      }
    }

    return healthyHosts;
  }

  /**
   * Updates the version states.  Transactional to ensure only one transaction for all updates
   * @param clusterId the cluster
   * @param current   the repository that is current for the cluster
   * @param target    the target repository
   */
  @Transactional
  protected void updateVersionStates(Long clusterId, RepositoryVersionEntity current,
      RepositoryVersionEntity target) {

    hostComponentStateDAO.updateVersions(target.getVersion());
    hostVersionDAO.updateVersions(target, current);
    clusterVersionDAO.updateVersions(clusterId, target, current);
  }

  /**
   * Additional check over {@link VersionUtils#compareVersions(String, String)} that
   * compares build numbers
   */
  private static int compareVersions(String version1, String version2) {
    // check _exact_ equality
    if (StringUtils.equals(version1, version2)) {
      return 0;
    }

    int compare = VersionUtils.compareVersions(version1, version2);
    if (0 != compare) {
      return compare;
    }

    int v1 = 0;
    int v2 = 0;
    if (version1.indexOf('-') > -1) {
      v1 = NumberUtils.toInt(version1.substring(version1.indexOf('-')), 0);
    }

    if (version2.indexOf('-') > -1) {
      v2 = NumberUtils.toInt(version2.substring(version2.indexOf('-')), 0);
    }

    compare = v2 - v1;

    return (compare == 0) ? 0 : (compare < 0) ? -1 : 1;
  }


}
