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

import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.HOOKS_FOLDER;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SERVICE_PACKAGE_FOLDER;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.agent.ExecutionCommand.KeyNames;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.ActionExecutionContext;
import org.apache.ambari.server.controller.AmbariActionExecutionHelper;
import org.apache.ambari.server.controller.AmbariCustomCommandExecutionHelper;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ExecuteCommandJson;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandStatusSummaryDTO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.orm.entities.UpgradeGroupEntity;
import org.apache.ambari.server.orm.entities.UpgradeHistoryEntity;
import org.apache.ambari.server.orm.entities.UpgradeItemEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.serveraction.upgrades.UpdateDesiredStackAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.UpgradeContext;
import org.apache.ambari.server.state.UpgradeContextFactory;
import org.apache.ambari.server.state.UpgradeHelper;
import org.apache.ambari.server.state.UpgradeHelper.UpgradeGroupHolder;
import org.apache.ambari.server.state.stack.ConfigUpgradePack;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.ambari.server.state.stack.upgrade.ConfigureTask;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.Grouping;
import org.apache.ambari.server.state.stack.upgrade.ManualTask;
import org.apache.ambari.server.state.stack.upgrade.ServerSideActionTask;
import org.apache.ambari.server.state.stack.upgrade.StageWrapper;
import org.apache.ambari.server.state.stack.upgrade.Task;
import org.apache.ambari.server.state.stack.upgrade.TaskWrapper;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostServerActionEvent;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.persist.Transactional;

/**
 * Manages the ability to start and get status of upgrades.
 */
@StaticallyInject
public class UpgradeResourceProvider extends AbstractControllerResourceProvider {

  public static final String UPGRADE_CLUSTER_NAME = "Upgrade/cluster_name";
  public static final String UPGRADE_REPO_VERSION_ID = "Upgrade/repository_version_id";
  public static final String UPGRADE_TYPE = "Upgrade/upgrade_type";
  public static final String UPGRADE_PACK = "Upgrade/pack";
  public static final String UPGRADE_REQUEST_ID = "Upgrade/request_id";
  public static final String UPGRADE_ASSOCIATED_VERSION = "Upgrade/associated_version";
  public static final String UPGRADE_VERSIONS = "Upgrade/versions";
  public static final String UPGRADE_DIRECTION = "Upgrade/direction";
  public static final String UPGRADE_DOWNGRADE_ALLOWED = "Upgrade/downgrade_allowed";
  public static final String UPGRADE_REQUEST_STATUS = "Upgrade/request_status";
  public static final String UPGRADE_SUSPENDED = "Upgrade/suspended";
  public static final String UPGRADE_ABORT_REASON = "Upgrade/abort_reason";
  public static final String UPGRADE_SKIP_PREREQUISITE_CHECKS = "Upgrade/skip_prerequisite_checks";
  public static final String UPGRADE_FAIL_ON_CHECK_WARNINGS = "Upgrade/fail_on_check_warnings";

  /**
   * Names that appear in the Upgrade Packs that are used by
   * {@link org.apache.ambari.server.state.cluster.ClusterImpl#isNonRollingUpgradePastUpgradingStack}
   * to determine if an upgrade has already changed the version to use.
   * For this reason, DO NOT CHANGE the name of these since they represent historic values.
   */
  public static final String CONST_UPGRADE_GROUP_NAME = "UPDATE_DESIRED_STACK_ID";
  public static final String CONST_UPGRADE_ITEM_TEXT = "Update Target Stack";
  public static final String CONST_CUSTOM_COMMAND_NAME = UpdateDesiredStackAction.class.getName();


  /**
   * Skip slave/client component failures if the tasks are skippable.
   */
  public static final String UPGRADE_SKIP_FAILURES = "Upgrade/skip_failures";

  /**
   * Skip service check failures if the tasks are skippable.
   */
  public static final String UPGRADE_SKIP_SC_FAILURES = "Upgrade/skip_service_check_failures";

  /**
   * Skip manual verification tasks for hands-free upgrade/downgrade experience.
   */
  public static final String UPGRADE_SKIP_MANUAL_VERIFICATION = "Upgrade/skip_manual_verification";

  /**
   * When creating an upgrade of type {@link UpgradeType#HOST_ORDERED}, this
   * specifies the order in which the hosts are upgraded.
   * </p>
   *
   * <pre>
   * "host_order": [
   *   { "hosts":
   *       [ "c6401.ambari.apache.org, "c6402.ambari.apache.org", "c6403.ambari.apache.org" ],
   *     "service_checks": ["ZOOKEEPER"]
   *   },
   *   {
   *     "hosts": [ "c6404.ambari.apache.org, "c6405.ambari.apache.org"],
   *     "service_checks": ["ZOOKEEPER", "KAFKA"]
   *   }
   * ]
   * </pre>
   *
   */
  public static final String UPGRADE_HOST_ORDERED_HOSTS = "Upgrade/host_order";

  /*
   * Lifted from RequestResourceProvider
   */
  private static final String REQUEST_CONTEXT_ID = "Upgrade/request_context";
  private static final String REQUEST_TYPE_ID = "Upgrade/type";
  private static final String REQUEST_CREATE_TIME_ID = "Upgrade/create_time";
  private static final String REQUEST_START_TIME_ID = "Upgrade/start_time";
  private static final String REQUEST_END_TIME_ID = "Upgrade/end_time";
  private static final String REQUEST_EXCLUSIVE_ID = "Upgrade/exclusive";

  private static final String REQUEST_PROGRESS_PERCENT_ID = "Upgrade/progress_percent";
  private static final String REQUEST_STATUS_PROPERTY_ID = "Upgrade/request_status";

  private static final Set<String> PK_PROPERTY_IDS = new HashSet<>(
      Arrays.asList(UPGRADE_REQUEST_ID, UPGRADE_CLUSTER_NAME));

  private static final Set<String> PROPERTY_IDS = new HashSet<>();

  /**
   * The list of supported services put on a command.
   */
  public static final String COMMAND_PARAM_SUPPORTED_SERVICES = "supported_services";

  private static final String DEFAULT_REASON_TEMPLATE = "Aborting upgrade %s";

  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = new HashMap<>();

  @Inject
  protected static UpgradeDAO s_upgradeDAO = null;

  @Inject
  private static Provider<AmbariMetaInfo> s_metaProvider = null;

  @Inject
  private static RepositoryVersionDAO s_repoVersionDAO = null;

  @Inject
  private static Provider<RequestFactory> s_requestFactory;

  @Inject
  private static Provider<StageFactory> s_stageFactory;

  @Inject
  private static Provider<Clusters> clusters = null;

  @Inject
  private static Provider<AmbariActionExecutionHelper> s_actionExecutionHelper;

  @Inject
  private static Provider<AmbariCustomCommandExecutionHelper> s_commandExecutionHelper;

  @Inject
  private static RequestDAO s_requestDAO = null;

  @Inject
  private static HostRoleCommandDAO s_hostRoleCommandDAO = null;

  /**
   * Used to generated the correct tasks and stages during an upgrade.
   */
  @Inject
  private static UpgradeHelper s_upgradeHelper;

  @Inject
  private static Configuration s_configuration;

  /**
   * Used to create instances of {@link UpgradeContext} with injected
   * dependencies.
   */
  @Inject
  private static UpgradeContextFactory s_upgradeContextFactory;

  static {
    // properties
    PROPERTY_IDS.add(UPGRADE_CLUSTER_NAME);
    PROPERTY_IDS.add(UPGRADE_REPO_VERSION_ID);
    PROPERTY_IDS.add(UPGRADE_TYPE);
    PROPERTY_IDS.add(UPGRADE_PACK);
    PROPERTY_IDS.add(UPGRADE_REQUEST_ID);
    PROPERTY_IDS.add(UPGRADE_ASSOCIATED_VERSION);
    PROPERTY_IDS.add(UPGRADE_VERSIONS);
    PROPERTY_IDS.add(UPGRADE_DIRECTION);
    PROPERTY_IDS.add(UPGRADE_DOWNGRADE_ALLOWED);
    PROPERTY_IDS.add(UPGRADE_SUSPENDED);
    PROPERTY_IDS.add(UPGRADE_SKIP_FAILURES);
    PROPERTY_IDS.add(UPGRADE_SKIP_SC_FAILURES);
    PROPERTY_IDS.add(UPGRADE_SKIP_MANUAL_VERIFICATION);
    PROPERTY_IDS.add(UPGRADE_SKIP_PREREQUISITE_CHECKS);
    PROPERTY_IDS.add(UPGRADE_FAIL_ON_CHECK_WARNINGS);
    PROPERTY_IDS.add(UPGRADE_HOST_ORDERED_HOSTS);

    PROPERTY_IDS.add(REQUEST_CONTEXT_ID);
    PROPERTY_IDS.add(REQUEST_CREATE_TIME_ID);
    PROPERTY_IDS.add(REQUEST_END_TIME_ID);
    PROPERTY_IDS.add(REQUEST_EXCLUSIVE_ID);
    PROPERTY_IDS.add(REQUEST_PROGRESS_PERCENT_ID);
    PROPERTY_IDS.add(REQUEST_START_TIME_ID);
    PROPERTY_IDS.add(REQUEST_STATUS_PROPERTY_ID);
    PROPERTY_IDS.add(REQUEST_TYPE_ID);

    PROPERTY_IDS.add("Upgrade/from_version");
    PROPERTY_IDS.add("Upgrade/to_version");

    // keys
    KEY_PROPERTY_IDS.put(Resource.Type.Upgrade, UPGRADE_REQUEST_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.Cluster, UPGRADE_CLUSTER_NAME);
  }

  private static final Logger LOG = LoggerFactory.getLogger(UpgradeResourceProvider.class);

  /**
   * Constructor.
   *
   * @param controller
   *          the controller
   */
  @Inject
  public UpgradeResourceProvider(@Assisted AmbariManagementController controller) {
    super(PROPERTY_IDS, KEY_PROPERTY_IDS, controller);
  }

  @Override
  public RequestStatus createResources(final Request request) throws SystemException,
      UnsupportedPropertyException, ResourceAlreadyExistsException, NoSuchParentResourceException {

    Set<Map<String, Object>> requestMaps = request.getProperties();

    if (requestMaps.size() > 1) {
      throw new SystemException("Can only initiate one upgrade per request.");
    }

    // !!! above check ensures only one
    final Map<String, Object> requestMap = requestMaps.iterator().next();
    final String clusterName = (String) requestMap.get(UPGRADE_CLUSTER_NAME);
    final Cluster cluster;

    try {
      cluster = clusters.get().getCluster(clusterName);
    } catch (AmbariException e) {
      throw new NoSuchParentResourceException(
          String.format("Cluster %s could not be loaded", clusterName));
    }

    if (!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, cluster.getResourceId(),
        EnumSet.of(RoleAuthorization.CLUSTER_UPGRADE_DOWNGRADE_STACK))) {
      throw new AuthorizationException("The authenticated user does not have authorization to " +
          "manage upgrade and downgrade");
    }

    UpgradeEntity entity = createResources(new Command<UpgradeEntity>() {
      @Override
      public UpgradeEntity invoke() throws AmbariException, AuthorizationException {

        // create the context, validating the properties in the process
        final UpgradeContext upgradeContext = s_upgradeContextFactory.create(cluster, requestMap);

        try {
          return createUpgrade(upgradeContext);
        } catch (Exception e) {
          LOG.error("Error appears during upgrade task submitting", e);

          // Any error caused in the createUpgrade will initiate transaction
          // rollback
          // As we operate inside with cluster data, any cache which belongs to
          // cluster need to be flushed
          clusters.get().invalidate(cluster);
          throw e;
        }
      }
    });

    if (null == entity) {
      throw new SystemException("Could not load upgrade");
    }

    notifyCreate(Resource.Type.Upgrade, request);

    Resource res = new ResourceImpl(Resource.Type.Upgrade);
    res.setProperty(UPGRADE_REQUEST_ID, entity.getRequestId());
    return new RequestStatusImpl(null, Collections.singleton(res));
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate) throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> results = new HashSet<>();
    Set<String> requestPropertyIds = getRequestPropertyIds(request, predicate);

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      String clusterName = (String) propertyMap.get(UPGRADE_CLUSTER_NAME);

      if (null == clusterName || clusterName.isEmpty()) {
        throw new IllegalArgumentException(
            "The cluster name is required when querying for upgrades");
      }

      Cluster cluster;
      try {
        cluster = clusters.get().getCluster(clusterName);
      } catch (AmbariException e) {
        throw new NoSuchResourceException(
            String.format("Cluster %s could not be loaded", clusterName));
      }

      List<UpgradeEntity> upgrades = new ArrayList<>();

      String upgradeIdStr = (String) propertyMap.get(UPGRADE_REQUEST_ID);
      if (null != upgradeIdStr) {
        UpgradeEntity upgrade = s_upgradeDAO.findUpgradeByRequestId(Long.valueOf(upgradeIdStr));

        if (null != upgrade) {
          upgrades.add(upgrade);
        }
      } else {
        upgrades = s_upgradeDAO.findUpgrades(cluster.getClusterId());
      }

      for (UpgradeEntity entity : upgrades) {
        Resource r = toResource(entity, clusterName, requestPropertyIds);
        results.add(r);

        RequestEntity rentity = s_requestDAO.findByPK(entity.getRequestId());

        setResourceProperty(r, REQUEST_CONTEXT_ID, rentity.getRequestContext(), requestPropertyIds);
        setResourceProperty(r, REQUEST_TYPE_ID, rentity.getRequestType(), requestPropertyIds);
        setResourceProperty(r, REQUEST_CREATE_TIME_ID, rentity.getCreateTime(), requestPropertyIds);
        setResourceProperty(r, REQUEST_START_TIME_ID, rentity.getStartTime(), requestPropertyIds);
        setResourceProperty(r, REQUEST_END_TIME_ID, rentity.getEndTime(), requestPropertyIds);
        setResourceProperty(r, REQUEST_EXCLUSIVE_ID, rentity.isExclusive(), requestPropertyIds);

        Map<Long, HostRoleCommandStatusSummaryDTO> summary = s_hostRoleCommandDAO.findAggregateCounts(
            entity.getRequestId());

        CalculatedStatus calc = CalculatedStatus.statusFromStageSummary(summary, summary.keySet());

        setResourceProperty(r, REQUEST_STATUS_PROPERTY_ID, calc.getStatus(), requestPropertyIds);
        setResourceProperty(r, REQUEST_PROGRESS_PERCENT_ID, calc.getPercent(), requestPropertyIds);
      }
    }

    return results;
  }

  @Override
  public RequestStatus updateResources(final Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException,
      NoSuchParentResourceException {

    Set<Map<String, Object>> requestMaps = request.getProperties();

    if (requestMaps.size() > 1) {
      throw new SystemException("Can only update one upgrade per request.");
    }

    // !!! above check ensures only one
    final Map<String, Object> propertyMap = requestMaps.iterator().next();

    final String clusterName = (String) propertyMap.get(UPGRADE_CLUSTER_NAME);
    final Cluster cluster;

    try {
      cluster = clusters.get().getCluster(clusterName);
    } catch (AmbariException e) {
      throw new NoSuchParentResourceException(
          String.format("Cluster %s could not be loaded", clusterName));
    }

    if (!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, cluster.getResourceId(),
        EnumSet.of(RoleAuthorization.CLUSTER_UPGRADE_DOWNGRADE_STACK))) {
      throw new AuthorizationException("The authenticated user does not have authorization to " +
          "manage upgrade and downgrade");
    }

    String requestIdProperty = (String) propertyMap.get(UPGRADE_REQUEST_ID);
    if (null == requestIdProperty) {
      throw new IllegalArgumentException(String.format("%s is required", UPGRADE_REQUEST_ID));
    }

    long requestId = Long.parseLong(requestIdProperty);
    UpgradeEntity upgradeEntity = s_upgradeDAO.findUpgradeByRequestId(requestId);
    if( null == upgradeEntity){
      String exceptionMessage = MessageFormat.format("The upgrade with request ID {0} was not found", requestIdProperty);
      throw new NoSuchParentResourceException(exceptionMessage);
    }

    // the properties which are allowed to be updated; the request must include
    // at least 1
    List<String> updatableProperties = Lists.newArrayList(UPGRADE_REQUEST_STATUS,
        UPGRADE_SKIP_FAILURES, UPGRADE_SKIP_SC_FAILURES);

    boolean isRequiredPropertyInRequest = CollectionUtils.containsAny(updatableProperties,
        propertyMap.keySet());

    if (!isRequiredPropertyInRequest) {
      String exceptionMessage = MessageFormat.format(
          "At least one of the following properties is required in the request: {0}",
          StringUtils.join(updatableProperties, ", "));
      throw new IllegalArgumentException(exceptionMessage);
    }

    String requestStatus = (String) propertyMap.get(UPGRADE_REQUEST_STATUS);
    String skipFailuresRequestProperty = (String) propertyMap.get(UPGRADE_SKIP_FAILURES);
    String skipServiceCheckFailuresRequestProperty = (String) propertyMap.get(UPGRADE_SKIP_SC_FAILURES);

    if (null != requestStatus) {
      HostRoleStatus status = HostRoleStatus.valueOf(requestStatus);

      // When aborting an upgrade, the suspend flag must be present to indicate
      // if the upgrade is merely being paused (suspended=true) or aborted to initiate a downgrade (suspended=false).
      boolean suspended = false;
      if (status == HostRoleStatus.ABORTED && !propertyMap.containsKey(UPGRADE_SUSPENDED)){
        throw new IllegalArgumentException(String.format(
            "When changing the state of an upgrade to %s, the %s property is required to be either true or false.",
            status, UPGRADE_SUSPENDED ));
      } else if (status == HostRoleStatus.ABORTED) {
        suspended = Boolean.valueOf((String) propertyMap.get(UPGRADE_SUSPENDED));
      }

      try {
        setUpgradeRequestStatus(cluster, requestId, status, suspended, propertyMap);
      } catch (AmbariException ambariException) {
        throw new SystemException(ambariException.getMessage(), ambariException);
      }
    }

    // if either of the skip failure settings are in the request, then we need
    // to iterate over the entire series of tasks anyway, so do them both at the
    // same time
    if (StringUtils.isNotEmpty(skipFailuresRequestProperty)
        || StringUtils.isNotEmpty(skipServiceCheckFailuresRequestProperty)) {
      // grab the current settings for both
      boolean skipFailures = upgradeEntity.isComponentFailureAutoSkipped();
      boolean skipServiceCheckFailures = upgradeEntity.isServiceCheckFailureAutoSkipped();

      if (null != skipFailuresRequestProperty) {
        skipFailures = Boolean.parseBoolean(skipFailuresRequestProperty);
      }

      if (null != skipServiceCheckFailuresRequestProperty) {
        skipServiceCheckFailures = Boolean.parseBoolean(skipServiceCheckFailuresRequestProperty);
      }

      s_hostRoleCommandDAO.updateAutomaticSkipOnFailure(requestId, skipFailures, skipServiceCheckFailures);

      upgradeEntity.setAutoSkipComponentFailures(skipFailures);
      upgradeEntity.setAutoSkipServiceCheckFailures(skipServiceCheckFailures);
      s_upgradeDAO.merge(upgradeEntity);

    }

    return getRequestStatus(null);
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate) throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new SystemException("Cannot delete Upgrades");
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return PK_PROPERTY_IDS;
  }

  private Resource toResource(UpgradeEntity entity, String clusterName, Set<String> requestedIds) {
    ResourceImpl resource = new ResourceImpl(Resource.Type.Upgrade);

    setResourceProperty(resource, UPGRADE_CLUSTER_NAME, clusterName, requestedIds);
    setResourceProperty(resource, UPGRADE_TYPE, entity.getUpgradeType(), requestedIds);
    setResourceProperty(resource, UPGRADE_PACK, entity.getUpgradePackage(), requestedIds);
    setResourceProperty(resource, UPGRADE_REQUEST_ID, entity.getRequestId(), requestedIds);
    setResourceProperty(resource, UPGRADE_DIRECTION, entity.getDirection(), requestedIds);
    setResourceProperty(resource, UPGRADE_SUSPENDED, entity.isSuspended(), requestedIds);
    setResourceProperty(resource, UPGRADE_DOWNGRADE_ALLOWED, entity.isDowngradeAllowed(), requestedIds);
    setResourceProperty(resource, UPGRADE_SKIP_FAILURES, entity.isComponentFailureAutoSkipped(), requestedIds);
    setResourceProperty(resource, UPGRADE_SKIP_SC_FAILURES, entity.isServiceCheckFailureAutoSkipped(), requestedIds);

    // set the assocaited to/from version (to/from is dictated by direction)
    RepositoryVersionEntity repositoryVersion = entity.getRepositoryVersion();
    setResourceProperty(resource, UPGRADE_ASSOCIATED_VERSION, repositoryVersion.getVersion(), requestedIds);

    // now set the target verison for all services in the upgrade
    Map<String, RepositoryVersions> repositoryVersions = new HashMap<>();
    for( UpgradeHistoryEntity history : entity.getHistory() ){
      RepositoryVersions serviceVersions = repositoryVersions.get(history.getServiceName());
      if (null != serviceVersions) {
        continue;
      }

      serviceVersions = new RepositoryVersions(history.getFromReposistoryVersion(),
          history.getTargetRepositoryVersion());

      repositoryVersions.put(history.getServiceName(), serviceVersions);
    }

    setResourceProperty(resource, UPGRADE_VERSIONS, repositoryVersions, requestedIds);

    return resource;
  }

  /**
   * Inject variables into the
   * {@link org.apache.ambari.server.orm.entities.UpgradeItemEntity}, whose
   * tasks may use strings like {{configType/propertyName}} that need to be
   * retrieved from the properties.
   *
   * @param configHelper
   *          Configuration Helper
   * @param cluster
   *          Cluster
   * @param upgradeItem
   *          the item whose tasks will be injected.
   */
  private void injectVariables(ConfigHelper configHelper, Cluster cluster,
      UpgradeItemEntity upgradeItem) {
    final String regexp = "(\\{\\{.*?\\}\\})";

    String task = upgradeItem.getTasks();
    if (task != null && !task.isEmpty()) {
      Matcher m = Pattern.compile(regexp).matcher(task);
      while (m.find()) {
        String origVar = m.group(1);
        String configValue = configHelper.getPlaceholderValueFromDesiredConfigurations(cluster,
            origVar);

        if (null != configValue) {
          task = task.replace(origVar, configValue);
        } else {
          LOG.error("Unable to retrieve value for {}", origVar);
        }

      }
      upgradeItem.setTasks(task);
    }
  }

  /**
   * Creates the upgrade. All Request/Stage/Task and Upgrade entities will exist
   * in the database when this method completes.
   * <p/>
   * This method itself must not be wrapped in a transaction since it can
   * potentially make 1000's of database calls while building the entities
   * before persisting them. This would create a long-lived transaction which
   * could lead to database deadlock issues. Instead, only the creation of the
   * actual entities is wrapped in a {@link Transactional} block.
   *
   * @param upgradeContext
   * @return
   * @throws AmbariException
   * @throws AuthorizationException
   */
  protected UpgradeEntity createUpgrade(UpgradeContext upgradeContext)
      throws AmbariException, AuthorizationException {

    UpgradePack pack = upgradeContext.getUpgradePack();
    Cluster cluster = upgradeContext.getCluster();
    Direction direction = upgradeContext.getDirection();

    ConfigHelper configHelper = getManagementController().getConfigHelper();

    List<UpgradeGroupHolder> groups = s_upgradeHelper.createSequence(pack, upgradeContext);

    if (groups.isEmpty()) {
      throw new AmbariException("There are no groupings available");
    }

    // Non Rolling Upgrades require a group with name "UPDATE_DESIRED_STACK_ID".
    // This is needed as a marker to indicate which version to use when an upgrade is paused.
    if (pack.getType() == UpgradeType.NON_ROLLING) {
      boolean foundGroupWithNameUPDATE_DESIRED_STACK_ID = false;
      for (UpgradeGroupHolder group : groups) {
        if (group.name.equalsIgnoreCase(CONST_UPGRADE_GROUP_NAME)) {
          foundGroupWithNameUPDATE_DESIRED_STACK_ID = true;
          break;
        }
      }

      if (foundGroupWithNameUPDATE_DESIRED_STACK_ID == false) {
        throw new AmbariException(String.format("NonRolling Upgrade Pack %s requires a Group with name %s",
            pack.getName(), CONST_UPGRADE_GROUP_NAME));
      }
    }

    List<UpgradeGroupEntity> groupEntities = new ArrayList<>();
    RequestStageContainer req = createRequest(upgradeContext);

    /**
    During a Rolling Upgrade, change the desired Stack Id if jumping across
    major stack versions (e.g., HDP 2.2 -> 2.3), and then set config changes
    so they are applied on the newer stack.

    During a {@link UpgradeType.NON_ROLLING} upgrade, the stack is applied during the middle of the upgrade (after
    stopping all services), and the configs are applied immediately before starting the services.
    The Upgrade Pack is responsible for calling {@link org.apache.ambari.server.serveraction.upgrades.UpdateDesiredStackAction}
    at the appropriate moment during the orchestration.
    **/
    if (pack.getType() == UpgradeType.ROLLING) {
      // Desired configs must be set before creating stages because the config tag
      // names are read and set on the command for filling in later
      applyStackAndProcessConfigurations(upgradeContext);

      // move component desired version and upgrade state
      s_upgradeHelper.putComponentsToUpgradingState(upgradeContext);
    }

    @Experimental(feature = ExperimentalFeature.PATCH_UPGRADES, comment = "This is wrong")
    StackId configurationPackSourceStackId = upgradeContext.getRepositoryVersion().getStackId();

    // resolve or build a proper config upgrade pack - always start out with the config pack
    // for the current stack and merge into that
    //
    // HDP 2.2 to 2.3 should start with the config-upgrade.xml from HDP 2.2
    // HDP 2.2 to 2.4 should start with HDP 2.2 and merge in HDP 2.3's config-upgrade.xml
    ConfigUpgradePack configUpgradePack = ConfigurationPackBuilder.build(pack,
        configurationPackSourceStackId);

    // create the upgrade and request
    for (UpgradeGroupHolder group : groups) {
      List<UpgradeItemEntity> itemEntities = new ArrayList<>();

      for (StageWrapper wrapper : group.items) {
        RepositoryVersionEntity effectiveRepositoryVersion = upgradeContext.getRepositoryVersion();

        if (wrapper.getType() == StageWrapper.Type.SERVER_SIDE_ACTION) {
          // !!! each stage is guaranteed to be of one type. but because there
          // is a bug that prevents one stage with multiple tasks assigned for
          // the same host, break them out into individual stages.
          for (TaskWrapper taskWrapper : wrapper.getTasks()) {
            for (Task task : taskWrapper.getTasks()) {
              if (upgradeContext.isManualVerificationAutoSkipped()
                  && task.getType() == Task.Type.MANUAL) {
                continue;
              }

              UpgradeItemEntity itemEntity = new UpgradeItemEntity();

              itemEntity.setText(wrapper.getText());
              itemEntity.setTasks(wrapper.getTasksJson());
              itemEntity.setHosts(wrapper.getHostsJson());
              itemEntities.add(itemEntity);

              injectVariables(configHelper, cluster, itemEntity);
              makeServerSideStage(group, upgradeContext, effectiveRepositoryVersion, req,
                  itemEntity, (ServerSideActionTask) task, configUpgradePack);
            }
          }
        } else {
          UpgradeItemEntity itemEntity = new UpgradeItemEntity();
          itemEntity.setText(wrapper.getText());
          itemEntity.setTasks(wrapper.getTasksJson());
          itemEntity.setHosts(wrapper.getHostsJson());
          itemEntities.add(itemEntity);

          injectVariables(configHelper, cluster, itemEntity);

          // upgrade items match a stage
          createStage(group, upgradeContext, effectiveRepositoryVersion, req, itemEntity, wrapper);
        }
      }

      if(!itemEntities.isEmpty()) {
        UpgradeGroupEntity groupEntity = new UpgradeGroupEntity();
        groupEntity.setName(group.name);
        groupEntity.setTitle(group.title);
        groupEntity.setItems(itemEntities);
        groupEntities.add(groupEntity);
      }
    }

    UpgradeEntity entity = new UpgradeEntity();
    entity.setRepositoryVersion(upgradeContext.getRepositoryVersion());
    entity.setUpgradeGroups(groupEntities);
    entity.setClusterId(cluster.getClusterId());
    entity.setDirection(direction);
    entity.setUpgradePackage(pack.getName());
    entity.setUpgradeType(pack.getType());
    entity.setAutoSkipComponentFailures(upgradeContext.isComponentFailureAutoSkipped());
    entity.setAutoSkipServiceCheckFailures(upgradeContext.isServiceCheckFailureAutoSkipped());

    if (upgradeContext.getDirection().isDowngrade()) {
      // !!! You can't downgrade a Downgrade, no matter what the upgrade pack says.
      entity.setDowngradeAllowed(false);
    } else {
      entity.setDowngradeAllowed(pack.isDowngradeAllowed());
    }

    // set upgrade history for every component in the upgrade
    Set<String> services = upgradeContext.getSupportedServices();
    for (String serviceName : services) {
      Service service = cluster.getService(serviceName);
      Map<String, ServiceComponent> componentMap = service.getServiceComponents();
      for (ServiceComponent component : componentMap.values()) {
        UpgradeHistoryEntity history = new UpgradeHistoryEntity();
        history.setUpgrade(entity);
        history.setServiceName(serviceName);
        history.setComponentName(component.getName());

        // depending on whether this is an upgrade or a downgrade, the history
        // will be different
        if (upgradeContext.getDirection() == Direction.UPGRADE) {
          history.setFromRepositoryVersion(component.getDesiredRepositoryVersion());
          history.setTargetRepositoryVersion(upgradeContext.getRepositoryVersion());
        } else {
          // the target version on a downgrade is the original version that the
          // service was on in the failed upgrade
          RepositoryVersionEntity targetRepositoryVersion =
              upgradeContext.getTargetRepositoryVersion(serviceName);

          history.setFromRepositoryVersion(upgradeContext.getRepositoryVersion());
          history.setTargetRepositoryVersion(targetRepositoryVersion);
        }

        // add the history
        entity.addHistory(history);
      }
    }

    req.getRequestStatusResponse();
    return createUpgradeInsideTransaction(cluster, req, entity);
  }

  /**
   * Creates the Request/Stage/Task entities and the Upgrade entities inside of
   * a single transaction. We break this out since the work to get us to this
   * point could take a very long time and involve many queries to the database
   * as the commands are being built.
   *
   * @param cluster
   *          the cluster (not {@code null}).
   * @param request
   *          the request to persist with all stages and tasks created in memory
   *          (not {@code null}).
   * @param upgradeEntity
   *          the upgrade to create and associate with the newly created request
   *          (not {@code null}).
   * @return the persisted {@link UpgradeEntity} encapsulating all
   *         {@link UpgradeGroupEntity} and {@link UpgradeItemEntity}.
   * @throws AmbariException
   */
  @Transactional
  UpgradeEntity createUpgradeInsideTransaction(Cluster cluster,
      RequestStageContainer request,
      UpgradeEntity upgradeEntity) throws AmbariException {

    request.persist();
    RequestEntity requestEntity = s_requestDAO.findByPK(request.getId());

    upgradeEntity.setRequestEntity(requestEntity);
    s_upgradeDAO.create(upgradeEntity);

    cluster.setUpgradeEntity(upgradeEntity);

    return upgradeEntity;
  }

  /**
   * Handles the creation or resetting of configurations based on whether an
   * upgrade or downgrade is occurring. This method will not do anything when
   * the target stack version is the same as the cluster's current stack version
   * since, by definition, no new configurations are automatically created when
   * upgrading with the same stack (ie HDP 2.2.0.0 -> HDP 2.2.1.0).
   * <p/>
   * When upgrading or downgrade between stacks (HDP 2.2.0.0 -> HDP 2.3.0.0)
   * then this will perform the following:
   * <ul>
   * <li>Upgrade: Create new configurations that are a merge between the current
   * stack and the desired stack. If a value has changed between stacks, then
   * the target stack value should be taken unless the cluster's value differs
   * from the old stack. This can occur if a property has been customized after
   * installation.</li>
   * <li>Downgrade: Reset the latest configurations from the cluster's original
   * stack. The new configurations that were created on upgrade must be left
   * intact until all components have been reverted, otherwise heartbeats will
   * fail due to missing configurations.</li>
   * </ul>
   *
   * @param upgradeContext  the upgrade context (not {@code null}).
   * @throws AmbariException
   */
  public void applyStackAndProcessConfigurations(UpgradeContext upgradeContext)
    throws AmbariException {

    Cluster cluster = upgradeContext.getCluster();
    Direction direction = upgradeContext.getDirection();
    UpgradePack upgradePack = upgradeContext.getUpgradePack();
    String stackName = upgradeContext.getRepositoryVersion().getStackId().getStackName();
    String version = upgradeContext.getRepositoryVersion().getStackId().getStackVersion();
    String userName = getManagementController().getAuthName();

    RepositoryVersionEntity targetRve = s_repoVersionDAO.findByStackNameAndVersion(stackName, version);
    if (null == targetRve) {
      LOG.info("Could not find version entity for {}; not setting new configs", version);
      return;
    }

    if (null == userName) {
      userName = getManagementController().getAuthName();
    }

    // if the current and target stacks are the same (ie HDP 2.2.0.0 -> 2.2.1.0)
    // then we should never do anything with configs on either upgrade or
    // downgrade; however if we are going across stacks, we have to do the stack
    // checks differently depending on whether this is an upgrade or downgrade
    StackEntity targetStack = targetRve.getStack();
    StackId currentStackId = cluster.getCurrentStackVersion();
    StackId desiredStackId = cluster.getDesiredStackVersion();
    StackId targetStackId = new StackId(targetStack);
    // Only change configs if moving to a different stack.
    switch (direction) {
      case UPGRADE:
        if (currentStackId.equals(targetStackId)) {
          return;
        }
        break;
      case DOWNGRADE:
        if (desiredStackId.equals(targetStackId)) {
          return;
        }
        break;
    }

    Map<String, Map<String, String>> newConfigurationsByType = null;
    ConfigHelper configHelper = getManagementController().getConfigHelper();

    if (direction == Direction.UPGRADE) {
      // populate a map of default configurations for the old stack (this is
      // used when determining if a property has been customized and should be
      // overriden with the new stack value)
      Map<String, Map<String, String>> oldStackDefaultConfigurationsByType = configHelper.getDefaultProperties(
          currentStackId, cluster, true);

      // populate a map with default configurations from the new stack
      newConfigurationsByType = configHelper.getDefaultProperties(targetStackId, cluster, true);

      // We want to skip updating config-types of services that are not in the upgrade pack.
      // Care should be taken as some config-types could be in services that are in and out
      // of the upgrade pack. We should never ignore config-types of services in upgrade pack.
      Set<String> skipConfigTypes = new HashSet<>();
      Set<String> upgradePackServices = new HashSet<>();
      Set<String> upgradePackConfigTypes = new HashSet<>();
      AmbariMetaInfo ambariMetaInfo = s_metaProvider.get();

      // ensure that we get the service info from the target stack
      // (since it could include new configuration types for a service)
      Map<String, ServiceInfo> stackServicesMap = ambariMetaInfo.getServices(
          targetStack.getStackName(), targetStack.getStackVersion());

      for (Grouping group : upgradePack.getGroups(direction)) {
        for (UpgradePack.OrderService service : group.services) {
          if (service.serviceName == null || upgradePackServices.contains(service.serviceName)) {
            // No need to re-process service that has already been looked at
            continue;
          }

          upgradePackServices.add(service.serviceName);
          ServiceInfo serviceInfo = stackServicesMap.get(service.serviceName);
          if (serviceInfo == null) {
            continue;
          }

          // add every configuration type for all services defined in the
          // upgrade pack
          Set<String> serviceConfigTypes = serviceInfo.getConfigTypeAttributes().keySet();
          for (String serviceConfigType : serviceConfigTypes) {
            if (!upgradePackConfigTypes.contains(serviceConfigType)) {
              upgradePackConfigTypes.add(serviceConfigType);
            }
          }
        }
      }

      // build a set of configurations that should not be merged since their
      // services are not installed
      Set<String> servicesNotInUpgradePack = new HashSet<>(stackServicesMap.keySet());
      servicesNotInUpgradePack.removeAll(upgradePackServices);
      for (String serviceNotInUpgradePack : servicesNotInUpgradePack) {
        ServiceInfo serviceInfo = stackServicesMap.get(serviceNotInUpgradePack);
        Set<String> configTypesOfServiceNotInUpgradePack = serviceInfo.getConfigTypeAttributes().keySet();
        for (String configType : configTypesOfServiceNotInUpgradePack) {
          if (!upgradePackConfigTypes.contains(configType) && !skipConfigTypes.contains(configType)) {
            skipConfigTypes.add(configType);
          }
        }
      }

      // remove any configurations from the target stack that are not used
      // because the services are not installed
      Iterator<String> iterator = newConfigurationsByType.keySet().iterator();
      while (iterator.hasNext()) {
        String configType = iterator.next();
        if (skipConfigTypes.contains(configType)) {
          LOG.info("Stack Upgrade: Removing configs for config-type {}", configType);
          iterator.remove();
        }
      }

      // now that the map has been populated with the default configurations
      // from the stack/service, overlay the existing configurations on top
      Map<String, DesiredConfig> existingDesiredConfigurationsByType = cluster.getDesiredConfigs();
      for (Map.Entry<String, DesiredConfig> existingEntry : existingDesiredConfigurationsByType.entrySet()) {
        String configurationType = existingEntry.getKey();
        if(skipConfigTypes.contains(configurationType)) {
          LOG.info("Stack Upgrade: Skipping config-type {} as upgrade-pack contains no updates to its service", configurationType);
          continue;
        }

        // NPE sanity, although shouldn't even happen since we are iterating
        // over the desired configs to start with
        Config currentClusterConfig = cluster.getDesiredConfigByType(configurationType);
        if (null == currentClusterConfig) {
          continue;
        }

        // get current stack default configurations on install
        Map<String, String> configurationTypeDefaultConfigurations = oldStackDefaultConfigurationsByType.get(
            configurationType);

        // NPE sanity for current stack defaults
        if (null == configurationTypeDefaultConfigurations) {
          configurationTypeDefaultConfigurations = Collections.emptyMap();
        }

        // get the existing configurations
        Map<String, String> existingConfigurations = currentClusterConfig.getProperties();

        // if the new stack configurations don't have the type, then simply add
        // all of the existing in
        Map<String, String> newDefaultConfigurations = newConfigurationsByType.get(
            configurationType);

        if (null == newDefaultConfigurations) {
          newConfigurationsByType.put(configurationType, existingConfigurations);
          continue;
        } else {
          // TODO, should we remove existing configs whose value is NULL even though they don't have a value in the new stack?

          // Remove any configs in the new stack whose value is NULL, unless they currently exist and the value is not NULL.
          Iterator<Map.Entry<String, String>> iter = newDefaultConfigurations.entrySet().iterator();
          while (iter.hasNext()) {
            Map.Entry<String, String> entry = iter.next();
            if (entry.getValue() == null) {
              iter.remove();
            }
          }
        }

        // for every existing configuration, see if an entry exists; if it does
        // not exist, then put it in the map, otherwise we'll have to compare
        // the existing value to the original stack value to see if its been
        // customized
        for (Map.Entry<String, String> existingConfigurationEntry : existingConfigurations.entrySet()) {
          String existingConfigurationKey = existingConfigurationEntry.getKey();
          String existingConfigurationValue = existingConfigurationEntry.getValue();

          // if there is already an entry, we now have to try to determine if
          // the value was customized after stack installation
          if (newDefaultConfigurations.containsKey(existingConfigurationKey)) {
            String newDefaultConfigurationValue = newDefaultConfigurations.get(
                existingConfigurationKey);

            if (!StringUtils.equals(existingConfigurationValue, newDefaultConfigurationValue)) {
              // the new default is different from the existing cluster value;
              // only override the default value if the existing value differs
              // from the original stack
              String oldDefaultValue = configurationTypeDefaultConfigurations.get(
                  existingConfigurationKey);

              if (!StringUtils.equals(existingConfigurationValue, oldDefaultValue)) {
                // at this point, we've determined that there is a difference
                // between default values between stacks, but the value was
                // also customized, so keep the customized value
                newDefaultConfigurations.put(existingConfigurationKey, existingConfigurationValue);
              }
            }
          } else {
            // there is no entry in the map, so add the existing key/value pair
            newDefaultConfigurations.put(existingConfigurationKey, existingConfigurationValue);
          }
        }

        /*
        for every new configuration which does not exist in the existing
        configurations, see if it was present in the current stack

        stack 2.x has foo-site/property (on-ambari-upgrade is false)
        stack 2.y has foo-site/property
        the current cluster (on 2.x) does not have it

        In this case, we should NOT add it back as clearly stack advisor has removed it
        */
        Iterator<Map.Entry<String, String>> newDefaultConfigurationsIterator = newDefaultConfigurations.entrySet().iterator();
        while( newDefaultConfigurationsIterator.hasNext() ){
          Map.Entry<String, String> newConfigurationEntry = newDefaultConfigurationsIterator.next();
          String newConfigurationPropertyName = newConfigurationEntry.getKey();
          if (configurationTypeDefaultConfigurations.containsKey(newConfigurationPropertyName)
              && !existingConfigurations.containsKey(newConfigurationPropertyName)) {
            LOG.info(
                "The property {}/{} exists in both {} and {} but is not part of the current set of configurations and will therefore not be included in the configuration merge",
                configurationType, newConfigurationPropertyName, currentStackId, targetStackId);

            // remove the property so it doesn't get merged in
            newDefaultConfigurationsIterator.remove();
          }
        }
      }
    } else {
      // downgrade
      cluster.applyLatestConfigurations(cluster.getCurrentStackVersion());
    }

    // !!! update the stack
    cluster.setDesiredStackVersion(
        new StackId(targetStack.getStackName(), targetStack.getStackVersion()));

    // !!! configs must be created after setting the stack version
    if (null != newConfigurationsByType) {
      configHelper.createConfigTypes(cluster, getManagementController(), newConfigurationsByType,
          userName, "Configuration created for Upgrade");
    }
  }

  private RequestStageContainer createRequest(UpgradeContext upgradeContext) {
    ActionManager actionManager = getManagementController().getActionManager();

    RequestStageContainer requestStages = new RequestStageContainer(
        actionManager.getNextRequestId(), null, s_requestFactory.get(), actionManager);

    Direction direction = upgradeContext.getDirection();
    RepositoryVersionEntity repositoryVersion = upgradeContext.getRepositoryVersion();

    requestStages.setRequestContext(String.format("%s %s %s", direction.getVerb(true),
        direction.getPreposition(), repositoryVersion.getVersion()));

    return requestStages;
  }

  private void createStage(UpgradeGroupHolder group, UpgradeContext context,
      RepositoryVersionEntity effectiveRepositoryVersion,
      RequestStageContainer request, UpgradeItemEntity entity, StageWrapper wrapper)
          throws AmbariException {

    boolean skippable = group.skippable;
    boolean supportsAutoSkipOnFailure = group.supportsAutoSkipOnFailure;
    boolean allowRetry = group.allowRetry;

    switch (wrapper.getType()) {
      case CONFIGURE:
      case START:
      case STOP:
      case RESTART:
        makeCommandStage(context, request, effectiveRepositoryVersion, entity, wrapper, skippable,
            supportsAutoSkipOnFailure, allowRetry);
        break;
      case RU_TASKS:
        makeActionStage(context, request, effectiveRepositoryVersion, entity, wrapper, skippable,
            supportsAutoSkipOnFailure, allowRetry);
        break;
      case SERVICE_CHECK:
        makeServiceCheckStage(context, request, effectiveRepositoryVersion, entity, wrapper,
            skippable, supportsAutoSkipOnFailure, allowRetry);
        break;
      default:
        break;
    }
  }

  /**
   * Modify the commandParams by applying additional parameters from the stage.
   * @param wrapper Stage Wrapper that may contain additional parameters.
   * @param commandParams Parameters to modify.
   */
  private void applyAdditionalParameters(StageWrapper wrapper, Map<String, String> commandParams) {
    if (wrapper.getParams() != null) {
      for (Map.Entry<String, String> pair : wrapper.getParams().entrySet()) {
        if (!commandParams.containsKey(pair.getKey())) {
          commandParams.put(pair.getKey(), pair.getValue());
        }
      }
    }
  }

  private void makeActionStage(UpgradeContext context, RequestStageContainer request,
      RepositoryVersionEntity effectiveRepositoryVersion, UpgradeItemEntity entity,
      StageWrapper wrapper, boolean skippable, boolean supportsAutoSkipOnFailure,
      boolean allowRetry) throws AmbariException {

    if (0 == wrapper.getHosts().size()) {
      throw new AmbariException(
          String.format("Cannot create action for '%s' with no hosts", wrapper.getText()));
    }

    Cluster cluster = context.getCluster();

    // add each host to this stage
    RequestResourceFilter filter = new RequestResourceFilter("", "",
        new ArrayList<>(wrapper.getHosts()));

    LOG.debug("Analyzing upgrade item {} with tasks: {}.", entity.getText(), entity.getTasks());
    Map<String, String> params = getNewParameterMap(request, context);
    params.put(UpgradeContext.COMMAND_PARAM_TASKS, entity.getTasks());

    // Apply additional parameters to the command that come from the stage.
    applyAdditionalParameters(wrapper, params);

    // Because custom task may end up calling a script/function inside a
    // service, it is necessary to set the
    // service_package_folder and hooks_folder params.
    AmbariMetaInfo ambariMetaInfo = s_metaProvider.get();
    StackId stackId = effectiveRepositoryVersion.getStackId();

    StackInfo stackInfo = ambariMetaInfo.getStack(stackId.getStackName(),
        stackId.getStackVersion());

    if (wrapper.getTasks() != null && wrapper.getTasks().size() > 0
        && wrapper.getTasks().get(0).getService() != null) {
      String serviceName = wrapper.getTasks().get(0).getService();
      ServiceInfo serviceInfo = ambariMetaInfo.getService(stackId.getStackName(),
          stackId.getStackVersion(), serviceName);

      params.put(SERVICE_PACKAGE_FOLDER, serviceInfo.getServicePackageFolder());
      params.put(HOOKS_FOLDER, stackInfo.getStackHooksFolder());
    }

    ActionExecutionContext actionContext = new ActionExecutionContext(cluster.getClusterName(),
        "ru_execute_tasks", Collections.singletonList(filter), params);

    // hosts in maintenance mode are excluded from the upgrade
    actionContext.setMaintenanceModeHostExcluded(true);

    actionContext.setTimeout(wrapper.getMaxTimeout(s_configuration));
    actionContext.setRetryAllowed(allowRetry);
    actionContext.setAutoSkipFailures(context.isComponentFailureAutoSkipped());

    ExecuteCommandJson jsons = s_commandExecutionHelper.get().getCommandJson(actionContext,
        cluster, effectiveRepositoryVersion);

    Stage stage = s_stageFactory.get().createNew(request.getId().longValue(), "/tmp/ambari",
        cluster.getClusterName(), cluster.getClusterId(), entity.getText(),
        jsons.getClusterHostInfo(), jsons.getCommandParamsForStage(),
        jsons.getHostParamsForStage());

    stage.setSkippable(skippable);
    stage.setAutoSkipFailureSupported(supportsAutoSkipOnFailure);

    long stageId = request.getLastStageId() + 1;
    if (0L == stageId) {
      stageId = 1L;
    }

    stage.setStageId(stageId);
    entity.setStageId(Long.valueOf(stageId));

    s_actionExecutionHelper.get().addExecutionCommandsToStage(actionContext, stage, null);

    // need to set meaningful text on the command
    for (Map<String, HostRoleCommand> map : stage.getHostRoleCommands().values()) {
      for (HostRoleCommand hrc : map.values()) {
        hrc.setCommandDetail(entity.getText());
      }
    }

    request.addStages(Collections.singletonList(stage));
  }

  /**
   * Used to create a stage for restart, start, or stop.
   * @param context Upgrade Context
   * @param request Container for stage
   * @param entity Upgrade Item
   * @param wrapper Stage
   * @param skippable Whether the item can be skipped
   * @param allowRetry Whether the item is allowed to be retried
   * @throws AmbariException
   */
  private void makeCommandStage(UpgradeContext context, RequestStageContainer request,
      RepositoryVersionEntity effectiveRepositoryVersion, UpgradeItemEntity entity,
      StageWrapper wrapper, boolean skippable, boolean supportsAutoSkipOnFailure,
      boolean allowRetry) throws AmbariException {

    Cluster cluster = context.getCluster();

    List<RequestResourceFilter> filters = new ArrayList<>();

    for (TaskWrapper tw : wrapper.getTasks()) {
      // add each host to this stage
      filters.add(new RequestResourceFilter(tw.getService(), tw.getComponent(),
          new ArrayList<>(tw.getHosts())));
    }

    String function = null;
    switch (wrapper.getType()) {
      case CONFIGURE:
      case START:
      case STOP:
      case RESTART:
        function = wrapper.getType().name();
        break;
      default:
        function = "UNKNOWN";
        break;
    }

    Map<String, String> commandParams = getNewParameterMap(request, context);

    // Apply additional parameters to the command that come from the stage.
    applyAdditionalParameters(wrapper, commandParams);

    ActionExecutionContext actionContext = new ActionExecutionContext(cluster.getClusterName(),
        function, filters, commandParams);
    actionContext.setTimeout(wrapper.getMaxTimeout(s_configuration));
    actionContext.setRetryAllowed(allowRetry);
    actionContext.setAutoSkipFailures(context.isComponentFailureAutoSkipped());

    // hosts in maintenance mode are excluded from the upgrade
    actionContext.setMaintenanceModeHostExcluded(true);

    ExecuteCommandJson jsons = s_commandExecutionHelper.get().getCommandJson(actionContext,
        cluster, effectiveRepositoryVersion);

    Stage stage = s_stageFactory.get().createNew(request.getId().longValue(), "/tmp/ambari",
        cluster.getClusterName(), cluster.getClusterId(), entity.getText(),
        jsons.getClusterHostInfo(), jsons.getCommandParamsForStage(),
        jsons.getHostParamsForStage());

    stage.setSkippable(skippable);
    stage.setAutoSkipFailureSupported(supportsAutoSkipOnFailure);

    long stageId = request.getLastStageId() + 1;
    if (0L == stageId) {
      stageId = 1L;
    }

    stage.setStageId(stageId);
    entity.setStageId(Long.valueOf(stageId));

    Map<String, String> requestParams = new HashMap<>();
    requestParams.put("command", function);

    // !!! it is unclear the implications of this on rolling or express upgrade.  To turn
    // this off, set "allow-retry" to false in the Upgrade Pack group
    if (allowRetry && context.getType() == UpgradeType.HOST_ORDERED) {
      requestParams.put(KeyNames.COMMAND_RETRY_ENABLED, Boolean.TRUE.toString().toLowerCase());
    }

    s_commandExecutionHelper.get().addExecutionCommandsToStage(actionContext, stage, requestParams);

    request.addStages(Collections.singletonList(stage));
  }

  private void makeServiceCheckStage(UpgradeContext context, RequestStageContainer request,
      RepositoryVersionEntity effectiveRepositoryVersion, UpgradeItemEntity entity,
      StageWrapper wrapper, boolean skippable, boolean supportsAutoSkipOnFailure,
      boolean allowRetry) throws AmbariException {

    List<RequestResourceFilter> filters = new ArrayList<>();

    for (TaskWrapper tw : wrapper.getTasks()) {
      filters.add(new RequestResourceFilter(tw.getService(), "", Collections.<String> emptyList()));
    }

    Cluster cluster = context.getCluster();

    Map<String, String> commandParams = getNewParameterMap(request, context);

    // Apply additional parameters to the command that come from the stage.
    applyAdditionalParameters(wrapper, commandParams);

    ActionExecutionContext actionContext = new ActionExecutionContext(cluster.getClusterName(),
        "SERVICE_CHECK", filters, commandParams);

    actionContext.setTimeout(wrapper.getMaxTimeout(s_configuration));
    actionContext.setRetryAllowed(allowRetry);
    actionContext.setAutoSkipFailures(context.isServiceCheckFailureAutoSkipped());

    // hosts in maintenance mode are excluded from the upgrade and should not be
    // candidates for service checks
    actionContext.setMaintenanceModeHostExcluded(true);

    ExecuteCommandJson jsons = s_commandExecutionHelper.get().getCommandJson(actionContext,
        cluster, effectiveRepositoryVersion);

    Stage stage = s_stageFactory.get().createNew(request.getId().longValue(), "/tmp/ambari",
        cluster.getClusterName(), cluster.getClusterId(), entity.getText(),
        jsons.getClusterHostInfo(), jsons.getCommandParamsForStage(),
        jsons.getHostParamsForStage());

    stage.setSkippable(skippable);
    stage.setAutoSkipFailureSupported(supportsAutoSkipOnFailure);

    long stageId = request.getLastStageId() + 1;
    if (0L == stageId) {
      stageId = 1L;
    }

    stage.setStageId(stageId);
    entity.setStageId(Long.valueOf(stageId));

    Map<String, String> requestParams = getNewParameterMap(request, context);
    s_commandExecutionHelper.get().addExecutionCommandsToStage(actionContext, stage, requestParams);

    request.addStages(Collections.singletonList(stage));
  }

  /**
   * Creates a stage consisting of server side actions
   * @param context upgrade context
   * @param request upgrade request
   * @param entity a single of upgrade
   * @param task server-side task (if any)
   * @param skippable if user can skip stage on failure
   * @param allowRetry if user can retry running stage on failure
   * @param configUpgradePack a runtime-generated config upgrade pack that
   * contains all config change definitions from all stacks involved into
   * upgrade
   * @throws AmbariException
   */
  private void makeServerSideStage(UpgradeGroupHolder group, UpgradeContext context,
      RepositoryVersionEntity effectiveRepositoryVersion, RequestStageContainer request,
      UpgradeItemEntity entity, ServerSideActionTask task, ConfigUpgradePack configUpgradePack)
      throws AmbariException {

    Cluster cluster = context.getCluster();
    UpgradePack upgradePack = context.getUpgradePack();

    Map<String, String> commandParams = getNewParameterMap(request, context);
    commandParams.put(UpgradeContext.COMMAND_PARAM_UPGRADE_PACK, upgradePack.getName());
    commandParams.put(COMMAND_PARAM_SUPPORTED_SERVICES, StringUtils.join(context.getSupportedServices(), ','));

    // Notice that this does not apply any params because the input does not specify a stage.
    // All of the other actions do use additional params.

    String itemDetail = entity.getText();
    String stageText = StringUtils.abbreviate(entity.getText(), 255);

    switch (task.getType()) {
      case SERVER_ACTION:
      case MANUAL: {
        ServerSideActionTask serverTask = task;

        if (null != serverTask.summary) {
          stageText = serverTask.summary;
        }

        if (task.getType() == Task.Type.MANUAL) {
          ManualTask mt = (ManualTask) task;

          if (StringUtils.isNotBlank(mt.structuredOut)) {
            commandParams.put(UpgradeContext.COMMAND_PARAM_STRUCT_OUT, mt.structuredOut);
          }
        }

        if (!serverTask.messages.isEmpty()) {
          JsonArray messageArray = new JsonArray();
          for (String message : serverTask.messages) {
            JsonObject messageObj = new JsonObject();
            messageObj.addProperty("message", message);
            messageArray.add(messageObj);
          }
          itemDetail = messageArray.toString();

          entity.setText(itemDetail);

          //To be used later on by the Stage...
          itemDetail = StringUtils.join(serverTask.messages, " ");
        }
        break;
      }
      case CONFIGURE: {
        ConfigureTask ct = (ConfigureTask) task;
        Map<String, String> configurationChanges =
                ct.getConfigurationChanges(cluster, configUpgradePack);

        // add all configuration changes to the command params
        commandParams.putAll(configurationChanges);

        // extract the config type to build the summary
        String configType = configurationChanges.get(ConfigureTask.PARAMETER_CONFIG_TYPE);
        if (null != configType) {
          itemDetail = String.format("Updating configuration %s", configType);
        } else {
          itemDetail = "Skipping Configuration Task "
              + StringUtils.defaultString(ct.id, "(missing id)");
        }

        entity.setText(itemDetail);

        String configureTaskSummary = ct.getSummary(configUpgradePack);
        if (null != configureTaskSummary) {
          stageText = configureTaskSummary;
        } else {
          stageText = itemDetail;
        }

        break;
      }
      default:
        break;
    }

    ActionExecutionContext actionContext = new ActionExecutionContext(cluster.getClusterName(),
        Role.AMBARI_SERVER_ACTION.toString(), Collections.<RequestResourceFilter> emptyList(),
        commandParams);

    actionContext.setTimeout(Short.valueOf((short) -1));
    actionContext.setRetryAllowed(group.allowRetry);
    actionContext.setAutoSkipFailures(context.isComponentFailureAutoSkipped());

    // hosts in maintenance mode are excluded from the upgrade
    actionContext.setMaintenanceModeHostExcluded(true);

    ExecuteCommandJson jsons = s_commandExecutionHelper.get().getCommandJson(actionContext,
        cluster, null);

    Stage stage = s_stageFactory.get().createNew(request.getId().longValue(), "/tmp/ambari",
        cluster.getClusterName(), cluster.getClusterId(), stageText, jsons.getClusterHostInfo(),
        jsons.getCommandParamsForStage(), jsons.getHostParamsForStage());

    stage.setSkippable(group.skippable);
    stage.setAutoSkipFailureSupported(group.supportsAutoSkipOnFailure);

    long stageId = request.getLastStageId() + 1;
    if (0L == stageId) {
      stageId = 1L;
    }

    stage.setStageId(stageId);
    entity.setStageId(Long.valueOf(stageId));

    stage.addServerActionCommand(task.getImplementationClass(),
        getManagementController().getAuthName(), Role.AMBARI_SERVER_ACTION, RoleCommand.EXECUTE,
        cluster.getClusterName(),
        new ServiceComponentHostServerActionEvent(null, System.currentTimeMillis()), commandParams,
        itemDetail, null, s_configuration.getDefaultServerTaskTimeout(), group.allowRetry,
        context.isComponentFailureAutoSkipped());

    request.addStages(Collections.singletonList(stage));
  }

  /**
   * Gets a map initialized with parameters required for upgrades to work. The
   * following properties are already set:
   * <ul>
   * <li>{@link UpgradeContext#COMMAND_PARAM_CLUSTER_NAME}
   * <li>{@link UpgradeContext#COMMAND_PARAM_DIRECTION}
   * <li>{@link UpgradeContext#COMMAND_PARAM_UPGRADE_TYPE}
   * <li>{@link KeyNames#REFRESH_CONFIG_TAGS_BEFORE_EXECUTION} - necessary in
   * order to have the commands contain the correct configurations. Otherwise,
   * they will contain the configurations that were available at the time the
   * command was created. For upgrades, this is problematic since the commands
   * are all created ahead of time, but the upgrade may change configs as part
   * of the upgrade pack.</li>
   * <li>{@link UpgradeContext#COMMAND_PARAM_REQUEST_ID}</li> the ID of the request.
   * <ul>
   *
   * @return the initialized parameter map.
   */
  private Map<String, String> getNewParameterMap(RequestStageContainer requestStageContainer,
      UpgradeContext context) {
    Map<String, String> parameters = context.getInitializedCommandParameters();
    parameters.put(UpgradeContext.COMMAND_PARAM_REQUEST_ID,
        String.valueOf(requestStageContainer.getId()));
    return parameters;
  }

  /**
   * Changes the status of the specified request for an upgrade. The valid
   * values are:
   * <ul>
   * <li>{@link HostRoleStatus#ABORTED}</li>
   * <li>{@link HostRoleStatus#PENDING}</li>
   * </ul>
   * This method will also adjust the cluster->upgrade association correctly
   * based on the new status being supplied.
   *
   * @param cluster
   *          the cluster
   * @param requestId
   *          the request to change the status for.
   * @param status
   *          the status to set on the associated request.
   * @param suspended
   *          if the value of the specified status is
   *          {@link HostRoleStatus#ABORTED}, then this boolean will control
   *          whether the upgrade is suspended (still associated with the
   *          cluster) or aborted (no longer associated with the cluster).
   * @param propertyMap
   *          the map of request properties (needed for things like abort reason
   *          if present)
   */
  @Transactional
  void setUpgradeRequestStatus(Cluster cluster, long requestId, HostRoleStatus status,
      boolean suspended, Map<String, Object> propertyMap) throws AmbariException {
    // these are the only two states we allow
    if (status != HostRoleStatus.ABORTED && status != HostRoleStatus.PENDING) {
      throw new IllegalArgumentException(String.format("Cannot set status %s, only %s is allowed",
          status, EnumSet.of(HostRoleStatus.ABORTED, HostRoleStatus.PENDING)));
    }

    String reason = (String) propertyMap.get(UPGRADE_ABORT_REASON);
    if (null == reason) {
      reason = String.format(DEFAULT_REASON_TEMPLATE, requestId);
    }

    // do not try to pull back the entire request here as they can be massive
    // and cause OOM problems; instead, use the count of statuses to determine
    // the state of the upgrade request
    Map<Long, HostRoleCommandStatusSummaryDTO> aggregateCounts = s_hostRoleCommandDAO.findAggregateCounts(requestId);
    CalculatedStatus calculatedStatus = CalculatedStatus.statusFromStageSummary(aggregateCounts,
        aggregateCounts.keySet());

    HostRoleStatus internalStatus = calculatedStatus.getStatus();

    if (HostRoleStatus.PENDING == status && !(internalStatus == HostRoleStatus.ABORTED || internalStatus == HostRoleStatus.IN_PROGRESS)) {
      throw new IllegalArgumentException(
          String.format("Can only set status to %s when the upgrade is %s (currently %s)", status,
              HostRoleStatus.ABORTED, internalStatus));
    }

    ActionManager actionManager = getManagementController().getActionManager();

    if (HostRoleStatus.ABORTED == status && !internalStatus.isCompletedState()) {
      // cancel the request
      actionManager.cancelRequest(requestId, reason);

      // either suspend the upgrade or abort it outright
      UpgradeEntity upgradeEntity = s_upgradeDAO.findUpgradeByRequestId(requestId);
      if (suspended) {
        // set the upgrade to suspended
        upgradeEntity.setSuspended(suspended);
        s_upgradeDAO.merge(upgradeEntity);
      } else {
        // otherwise remove the association with the cluster since it's being
        // full aborted
        cluster.setUpgradeEntity(null);
      }

    } else if (status == HostRoleStatus.PENDING) {
      List<Long> taskIds = new ArrayList<>();
      List<HostRoleCommandEntity> hrcEntities = s_hostRoleCommandDAO.findByRequestIdAndStatuses(
          requestId, Sets.newHashSet(HostRoleStatus.ABORTED, HostRoleStatus.TIMEDOUT));

      for (HostRoleCommandEntity hrcEntity : hrcEntities) {
        taskIds.add(hrcEntity.getTaskId());
      }

      actionManager.resubmitTasks(taskIds);

      UpgradeEntity lastUpgradeItemForCluster = s_upgradeDAO.findLastUpgradeOrDowngradeForCluster(cluster.getClusterId());
      lastUpgradeItemForCluster.setSuspended(false);
      s_upgradeDAO.merge(lastUpgradeItemForCluster);
    }
  }

  /**
   * Builds the correct {@link ConfigUpgradePack} based on the upgrade and
   * source stack.
   * <ul>
   * <li>HDP 2.2 to HDP 2.2
   * <ul>
   * <li>Uses {@code config-upgrade.xml} from HDP 2.2
   * </ul>
   * <li>HDP 2.2 to HDP 2.3
   * <ul>
   * <li>Uses {@code config-upgrade.xml} from HDP 2.2
   * </ul>
   * <li>HDP 2.2 to HDP 2.4
   * <ul>
   * <li>Uses {@code config-upgrade.xml} from HDP 2.2 merged with the one from
   * HDP 2.3
   * </ul>
   * <ul>
   */
  public static final class ConfigurationPackBuilder {

    /**
     * Builds the configurations to use for the specified upgrade and source
     * stack.
     *
     * @param upgradePack
     *          the upgrade pack (not {@code null}).
     * @param sourceStackId
     *          the source stack (not {@code null}).
     * @return the {@link ConfigUpgradePack} which contains all of the necessary
     *         configuration definitions for the upgrade.
     */
    public static ConfigUpgradePack build(UpgradePack upgradePack, StackId sourceStackId) {
      List<UpgradePack.IntermediateStack> intermediateStacks = upgradePack.getIntermediateStacks();
      ConfigUpgradePack configUpgradePack = s_metaProvider.get().getConfigUpgradePack(
          sourceStackId.getStackName(), sourceStackId.getStackVersion());

      // merge in any intermediate stacks
      if (null != intermediateStacks) {

        // start out with the source stack's config pack
        ArrayList<ConfigUpgradePack> configPacksToMerge = Lists.newArrayList(configUpgradePack);

        for (UpgradePack.IntermediateStack intermediateStack : intermediateStacks) {
          ConfigUpgradePack intermediateConfigUpgradePack = s_metaProvider.get().getConfigUpgradePack(
              sourceStackId.getStackName(), intermediateStack.version);

          configPacksToMerge.add(intermediateConfigUpgradePack);
        }

        // merge all together
        configUpgradePack = ConfigUpgradePack.merge(configPacksToMerge);
      }

      return configUpgradePack;
    }
  }

  /**
   * The {@link RepositoryVersions} class is used to represent to/from versions
   * of a service during an upgrade or downgrade.
   */
  final static class RepositoryVersions {
    @JsonProperty("from_repository_id")
    final long fromRepositoryId;

    @JsonProperty("from_repository_version")
    final String fromRepositoryVersion;

    @JsonProperty("to_repository_id")
    final long toRepositoryId;

    @JsonProperty("to_repository_version")
    final String toRepositoryVersion;

    /**
     * Constructor.
     *
     * @param from
     * @param target
     */
    public RepositoryVersions(RepositoryVersionEntity from, RepositoryVersionEntity to) {
      fromRepositoryId = from.getId();
      fromRepositoryVersion = from.getVersion();

      toRepositoryId = to.getId();
      toRepositoryVersion = to.getVersion();
    }
  }
}
