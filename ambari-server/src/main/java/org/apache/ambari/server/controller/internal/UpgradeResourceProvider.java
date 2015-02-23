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
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.JDK_LOCATION;
import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.SERVICE_PACKAGE_FOLDER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.apache.ambari.server.api.resources.UpgradeResourceDefinition;
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
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.orm.entities.UpgradeGroupEntity;
import org.apache.ambari.server.orm.entities.UpgradeItemEntity;
import org.apache.ambari.server.stack.MasterHostResolver;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.UpgradeContext;
import org.apache.ambari.server.state.UpgradeHelper;
import org.apache.ambari.server.state.UpgradeHelper.UpgradeGroupHolder;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.ambari.server.state.stack.upgrade.ConfigureTask;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.ManualTask;
import org.apache.ambari.server.state.stack.upgrade.ServerSideActionTask;
import org.apache.ambari.server.state.stack.upgrade.StageWrapper;
import org.apache.ambari.server.state.stack.upgrade.Task;
import org.apache.ambari.server.state.stack.upgrade.TaskWrapper;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostServerActionEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Manages the ability to start and get status of upgrades.
 */
@StaticallyInject
public class UpgradeResourceProvider extends AbstractControllerResourceProvider {

  protected static final String UPGRADE_CLUSTER_NAME = "Upgrade/cluster_name";
  protected static final String UPGRADE_VERSION = "Upgrade/repository_version";
  protected static final String UPGRADE_REQUEST_ID = "Upgrade/request_id";
  // TODO : Get rid of the UPGRADE_FORCE_DOWNGRADE property... should use downgrade create directive
  protected static final String UPGRADE_FORCE_DOWNGRADE = "Upgrade/force_downgrade";
  protected static final String UPGRADE_FROM_VERSION = "Upgrade/from_version";
  protected static final String UPGRADE_TO_VERSION = "Upgrade/to_version";
  protected static final String UPGRADE_DIRECTION = "Upgrade/direction";
  protected static final String UPGRADE_REQUEST_STATUS = "Upgrade/request_status";
  protected static final String UPGRADE_ABORT_REASON = "Upgrade/abort_reason";

  private static final Set<String> PK_PROPERTY_IDS = new HashSet<String>(
      Arrays.asList(UPGRADE_REQUEST_ID, UPGRADE_CLUSTER_NAME));
  private static final Set<String> PROPERTY_IDS = new HashSet<String>();

  private static final String COMMAND_PARAM_VERSION = "version";
  private static final String COMMAND_PARAM_CLUSTER_NAME = "clusterName";
  private static final String COMMAND_PARAM_DIRECTION = "upgrade_direction";
  private static final String COMMAND_PARAM_RESTART_TYPE = "restart_type";
  private static final String COMMAND_PARAM_TASKS = "tasks";
  private static final String COMMAND_PARAM_STRUCT_OUT = "structured_out";

  private static final String DEFAULT_REASON_TEMPLATE = "Aborting upgrade %s";

  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = new HashMap<Resource.Type, String>();

  @Inject
  private static UpgradeDAO s_upgradeDAO = null;

  @Inject
  private static Provider<AmbariMetaInfo> s_metaProvider = null;

  @Inject
  private static RepositoryVersionDAO s_repoVersionDAO = null;

  @Inject
  private static Provider<RequestFactory> s_requestFactory;

  @Inject
  private static Provider<StageFactory> s_stageFactory;

  @Inject
  private static Provider<AmbariActionExecutionHelper> s_actionExecutionHelper;

  @Inject
  private static Provider<AmbariCustomCommandExecutionHelper> s_commandExecutionHelper;

  /**
   * Used to generated the correct tasks and stages during an upgrade.
   */
  @Inject
  private static UpgradeHelper s_upgradeHelper;

  @Inject
  private static Configuration s_configuration;

  private static Map<String, String> REQUEST_PROPERTY_MAP = new HashMap<String, String>();

  static {
    // properties
    PROPERTY_IDS.add(UPGRADE_CLUSTER_NAME);
    PROPERTY_IDS.add(UPGRADE_VERSION);
    PROPERTY_IDS.add(UPGRADE_REQUEST_ID);
    PROPERTY_IDS.add(UPGRADE_FORCE_DOWNGRADE);
    PROPERTY_IDS.add(UPGRADE_FROM_VERSION);
    PROPERTY_IDS.add(UPGRADE_TO_VERSION);
    PROPERTY_IDS.add(UPGRADE_DIRECTION);

    // !!! boo
    for (String requestPropertyId : RequestResourceProvider.PROPERTY_IDS) {
      REQUEST_PROPERTY_MAP.put(requestPropertyId, requestPropertyId.replace("Requests/", "Upgrade/"));
    }
    PROPERTY_IDS.addAll(REQUEST_PROPERTY_MAP.values());

    // keys
    KEY_PROPERTY_IDS.put(Resource.Type.Upgrade, UPGRADE_REQUEST_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.Cluster, UPGRADE_CLUSTER_NAME);
  }

  private static final Logger LOG =
      LoggerFactory.getLogger(UpgradeResourceProvider.class);

  /**
   * Constructor.
   *
   * @param controller  the controller
   */
  UpgradeResourceProvider(AmbariManagementController controller) {
    super(PROPERTY_IDS, KEY_PROPERTY_IDS, controller);
  }

  @Override
  public RequestStatus createResources(final Request request)
      throws SystemException,
      UnsupportedPropertyException, ResourceAlreadyExistsException,
      NoSuchParentResourceException {

    Set<Map<String, Object>> requestMaps = request.getProperties();

    if (requestMaps.size() > 1) {
      throw new SystemException("Can only initiate one upgrade per request.");
    }

    // !!! above check ensures only one
    final Map<String, Object> requestMap = requestMaps.iterator().next();
    final Map<String, String> requestInfoProps = request.getRequestInfoProperties();

    UpgradeEntity entity = createResources(new Command<UpgradeEntity>() {
        @Override
        public UpgradeEntity invoke() throws AmbariException {
          String forceDowngrade = requestInfoProps.get(UpgradeResourceDefinition.DOWNGRADE_DIRECTIVE);

          // check the property if the directive is not specified...
          if (forceDowngrade == null) {
            forceDowngrade = (String) requestMap.get(UPGRADE_FORCE_DOWNGRADE);
          }

          Direction direction = Boolean.parseBoolean(forceDowngrade) ?
              Direction.DOWNGRADE : Direction.UPGRADE;

          UpgradePack up = validateRequest(direction, requestMap);

          return createUpgrade(direction, up, requestMap);
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
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> results = new HashSet<Resource>();
    Set<String> requestPropertyIds = getRequestPropertyIds(request, predicate);

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      String clusterName = (String) propertyMap.get(UPGRADE_CLUSTER_NAME);

      if (null == clusterName || clusterName.isEmpty()) {
        throw new IllegalArgumentException("The cluster name is required when querying for upgrades");
      }

      Cluster cluster;
      try {
        cluster = getManagementController().getClusters().getCluster(clusterName);
      } catch (AmbariException e) {
        throw new NoSuchResourceException(String.format("Cluster %s could not be loaded", clusterName));
      }

      List<UpgradeEntity> upgrades = new ArrayList<UpgradeEntity>();

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

        // !!! not terribly efficient, but that's ok in this case.  The handful-per-year
        // an upgrade is done won't kill performance.
        Resource r1 = s_upgradeHelper.getRequestResource(clusterName,
            entity.getRequestId());
        for (Entry<String, String> entry : REQUEST_PROPERTY_MAP.entrySet()) {
          Object o = r1.getPropertyValue(entry.getKey());

          setResourceProperty(r, entry.getValue(), o, requestPropertyIds);
        }
      }
    }

    return results;
  }

  @Override
  public RequestStatus updateResources(final Request request,
      Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Set<Map<String, Object>> requestMaps = request.getProperties();

    if (requestMaps.size() > 1) {
      throw new SystemException("Can only update one upgrade per request.");
    }

    // !!! above check ensures only one
    final Map<String, Object> propertyMap = requestMaps.iterator().next();

    String requestId = (String) propertyMap.get(UPGRADE_REQUEST_ID);
    if (null == requestId) {
      throw new IllegalArgumentException(String.format("%s is required", UPGRADE_REQUEST_ID));
    }

    String requestStatus = (String) propertyMap.get(UPGRADE_REQUEST_STATUS);
    if (null == requestStatus) {
      throw new IllegalArgumentException(String.format("%s is required", UPGRADE_REQUEST_STATUS));
    }

    HostRoleStatus status = HostRoleStatus.valueOf(requestStatus);
    if (status != HostRoleStatus.ABORTED) {
      throw new IllegalArgumentException(
          String.format("Cannot set status %s, only %s is allowed",
          status, HostRoleStatus.ABORTED));
    }

    String reason = (String) propertyMap.get(UPGRADE_ABORT_REASON);
    if (null == reason) {
      reason = String.format(DEFAULT_REASON_TEMPLATE, requestId);
    }

    ActionManager actionManager = getManagementController().getActionManager();
    List<org.apache.ambari.server.actionmanager.Request> requests =
        actionManager.getRequests(Collections.singletonList(Long.valueOf(requestId)));

    org.apache.ambari.server.actionmanager.Request internalRequest = requests.get(0);

    HostRoleStatus internalStatus = CalculatedStatus.statusFromStages(
      internalRequest.getStages()).getStatus();

    if (!internalStatus.isCompletedState()) {
      actionManager.cancelRequest(internalRequest.getRequestId(), reason);
    }

    return getRequestStatus(null);
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
    throw new SystemException("Cannot delete Upgrades");
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return PK_PROPERTY_IDS;
  }

  private Resource toResource(UpgradeEntity entity, String clusterName,
      Set<String> requestedIds) {
    ResourceImpl resource = new ResourceImpl(Resource.Type.Upgrade);

    setResourceProperty(resource, UPGRADE_CLUSTER_NAME, clusterName, requestedIds);
    setResourceProperty(resource, UPGRADE_REQUEST_ID, entity.getRequestId(), requestedIds);
    setResourceProperty(resource, UPGRADE_FROM_VERSION, entity.getFromVersion(), requestedIds);
    setResourceProperty(resource, UPGRADE_TO_VERSION, entity.getToVersion(), requestedIds);
    setResourceProperty(resource, UPGRADE_DIRECTION, entity.getDirection(), requestedIds);

    return resource;
  }

  /**
   * Validates a singular API request.
   *
   * @param requestMap the map of properties
   * @return the validated upgrade pack
   * @throws AmbariException
   */
  private UpgradePack validateRequest(Direction direction, Map<String, Object> requestMap)
      throws AmbariException {
    String clusterName = (String) requestMap.get(UPGRADE_CLUSTER_NAME);
    String version = (String) requestMap.get(UPGRADE_VERSION);
    String versionForUpgradePack = (String) requestMap.get(UPGRADE_FROM_VERSION);


    if (null == clusterName) {
      throw new AmbariException(String.format("%s is required", UPGRADE_CLUSTER_NAME));
    }

    if (null == version) {
      throw new AmbariException(String.format("%s is required", UPGRADE_VERSION));
    }

    Cluster cluster = getManagementController().getClusters().getCluster(clusterName);
    StackId stack = cluster.getDesiredStackVersion();

    String repoVersion = version;

    if (direction.isDowngrade() && null != versionForUpgradePack) {
      repoVersion = versionForUpgradePack;
    }

    RepositoryVersionEntity versionEntity = s_repoVersionDAO.findByStackAndVersion(
        stack.getStackId(), repoVersion);

    if (null == versionEntity) {
      throw new AmbariException(String.format("Version %s for stack %s was not found",
          repoVersion, stack.getStackVersion()));
    }

    Map<String, UpgradePack> packs = s_metaProvider.get().getUpgradePacks(
        stack.getStackName(), stack.getStackVersion());

    UpgradePack up = packs.get(versionEntity.getUpgradePackage());

    if (null == up) {
      throw new AmbariException(String.format(
          "Unable to perform %s.  Could not locate upgrade pack %s for version %s",
          direction.getText(false),
          versionEntity.getUpgradePackage(),
          repoVersion));
    }

    // !!! validate all hosts have the version installed

    return up;
  }

  /**
   * Inject variables into the {@link org.apache.ambari.server.orm.entities.UpgradeItemEntity}, whose
   * tasks may use strings like {{configType/propertyName}} that need to be retrieved from the properties.
   * @param configHelper Configuration Helper
   * @param cluster Cluster
   * @param upgradeItem the item whose tasks will be injected.
   */
  private void injectVariables(ConfigHelper configHelper, Cluster cluster,
      UpgradeItemEntity upgradeItem) {
    final String regexp = "(\\{\\{.*?\\}\\})";

    String task = upgradeItem.getTasks();
    if (task != null && !task.isEmpty()) {
      Matcher m = Pattern.compile(regexp).matcher(task);
      while (m.find()) {
        String origVar = m.group(1);
        String configValue = configHelper.getPlaceholderValueFromDesiredConfigurations(
            cluster, origVar);

        if (null != configValue) {
          task = task.replace(origVar, configValue);
        } else {
          LOG.error("Unable to retrieve value for {}", origVar);
        }

      }
      upgradeItem.setTasks(task);
    }
  }

  private UpgradeEntity createUpgrade(Direction direction, UpgradePack pack,
                                      Map<String, Object> requestMap) throws AmbariException {

    String clusterName = (String) requestMap.get(UPGRADE_CLUSTER_NAME);

    if (null == clusterName) {
      throw new AmbariException(String.format("%s is required", UPGRADE_CLUSTER_NAME));
    }

    Cluster cluster = getManagementController().getClusters().getCluster(clusterName);
    ConfigHelper configHelper = getManagementController().getConfigHelper();

    // the version being upgraded or downgraded to (ie hdp-2.2.1.0-1234)
    final String version = (String) requestMap.get(UPGRADE_VERSION);

    MasterHostResolver resolver = direction.isUpgrade() ?
        new MasterHostResolver(configHelper, cluster) : new MasterHostResolver(configHelper, cluster, version);

    UpgradeContext ctx = new UpgradeContext(resolver, version, direction);

    List<UpgradeGroupHolder> groups = s_upgradeHelper.createSequence(pack, ctx);

    if (groups.isEmpty()) {
      throw new AmbariException("There are no groupings available");
    }

    List<UpgradeGroupEntity> groupEntities = new ArrayList<UpgradeGroupEntity>();
    RequestStageContainer req = createRequest(direction, version);

    for (UpgradeGroupHolder group : groups) {
      UpgradeGroupEntity groupEntity = new UpgradeGroupEntity();
      groupEntity.setName(group.name);
      groupEntity.setTitle(group.title);
      boolean skippable = group.skippable;
      boolean allowRetry = group.allowRetry;

      List<UpgradeItemEntity> itemEntities = new ArrayList<UpgradeItemEntity>();

      for (StageWrapper wrapper : group.items) {
        if (wrapper.getType() == StageWrapper.Type.SERVER_SIDE_ACTION) {
          // !!! each stage is guaranteed to be of one type.  but because there
          // is a bug that prevents one stage with multiple tasks assigned for the same host,
          // break them out into individual stages.

          for (TaskWrapper taskWrapper : wrapper.getTasks()) {
            for (Task task : taskWrapper.getTasks()) {
              UpgradeItemEntity itemEntity = new UpgradeItemEntity();
              itemEntity.setText(wrapper.getText());
              itemEntity.setTasks(wrapper.getTasksJson());
              itemEntity.setHosts(wrapper.getHostsJson());
              itemEntities.add(itemEntity);

              injectVariables(configHelper, cluster, itemEntity);

              makeServerSideStage(ctx, req, itemEntity, (ServerSideActionTask) task, skippable, allowRetry);
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
          createStage(ctx, req, itemEntity, wrapper, skippable, allowRetry);
        }
      }

      groupEntity.setItems(itemEntities);

      groupEntities.add(groupEntity);

    }

    UpgradeEntity entity = new UpgradeEntity();
    entity.setFromVersion(cluster.getCurrentClusterVersion().getRepositoryVersion().getVersion());
    entity.setToVersion(version);
    entity.setUpgradeGroups(groupEntities);
    entity.setClusterId(Long.valueOf(cluster.getClusterId()));
    entity.setDirection(direction);

    req.getRequestStatusResponse();

    entity.setRequestId(req.getId());

    req.persist();

    s_upgradeDAO.create(entity);

    return entity;
  }


  private RequestStageContainer createRequest(Direction direction, String version) {
    ActionManager actionManager = getManagementController().getActionManager();

    RequestStageContainer requestStages = new RequestStageContainer(
        actionManager.getNextRequestId(), null, s_requestFactory.get(), actionManager);
    requestStages.setRequestContext(String.format("%s to %s",
        direction.getVerb(true), version));

    return requestStages;
  }

  private void createStage(UpgradeContext context, RequestStageContainer request,
      UpgradeItemEntity entity, StageWrapper wrapper, boolean skippable, boolean allowRetry)
      throws AmbariException {

    switch (wrapper.getType()) {
      case RESTART:
        makeRestartStage(context, request, entity, wrapper, skippable, allowRetry);
        break;
      case RU_TASKS:
        makeActionStage(context, request, entity, wrapper, skippable, allowRetry);
        break;
      case SERVICE_CHECK:
        makeServiceCheckStage(context, request, entity, wrapper, skippable, allowRetry);
        break;
      default:
        break;
    }
  }

  private void makeActionStage(UpgradeContext context, RequestStageContainer request,
                               UpgradeItemEntity entity, StageWrapper wrapper,
                               boolean skippable, boolean allowRetry) throws AmbariException {

    if (0 == wrapper.getHosts().size()) {
      throw new AmbariException(
          String.format("Cannot create action for '%s' with no hosts", wrapper.getText()));
    }

    Cluster cluster = context.getCluster();

    // add each host to this stage
    RequestResourceFilter filter = new RequestResourceFilter("", "",
        new ArrayList<String>(wrapper.getHosts()));

    Map<String, String> params = new HashMap<String, String>();
    params.put(COMMAND_PARAM_TASKS, entity.getTasks());
    params.put(COMMAND_PARAM_VERSION, context.getVersion());
    params.put(COMMAND_PARAM_DIRECTION, context.getDirection().name().toLowerCase());

    // Because custom task may end up calling a script/function inside a service, it is necessary to set the
    // service_package_folder and hooks_folder params.
    AmbariMetaInfo ambariMetaInfo = s_metaProvider.get();
    StackId stackId = cluster.getDesiredStackVersion();
    StackInfo stackInfo = ambariMetaInfo.getStack(stackId.getStackName(), stackId.getStackVersion());
    if (wrapper.getTasks() != null && wrapper.getTasks().size() > 0) {
      String serviceName = wrapper.getTasks().get(0).getService();
      ServiceInfo serviceInfo = ambariMetaInfo.getService(stackId.getStackName(), stackId.getStackVersion(), serviceName);
      params.put(SERVICE_PACKAGE_FOLDER,
          serviceInfo.getServicePackageFolder());
      params.put(HOOKS_FOLDER, stackInfo.getStackHooksFolder());
    }

    ActionExecutionContext actionContext = new ActionExecutionContext(
        cluster.getClusterName(), "ru_execute_tasks",
        Collections.singletonList(filter),
        params);
    actionContext.setIgnoreMaintenance(true);
    actionContext.setTimeout(Short.valueOf(s_configuration.getDefaultAgentTaskTimeout(false)));

    Map<String, String> hostLevelParams = new HashMap<String, String>();
    hostLevelParams.put(JDK_LOCATION, getManagementController().getJdkResourceUrl());

    ExecuteCommandJson jsons = s_commandExecutionHelper.get().getCommandJson(
        actionContext, cluster);

    Stage stage = s_stageFactory.get().createNew(request.getId().longValue(),
        "/tmp/ambari",
        cluster.getClusterName(),
        cluster.getClusterId(),
        entity.getText(),
        jsons.getClusterHostInfo(),
        jsons.getCommandParamsForStage(),
        jsons.getHostParamsForStage());

    stage.setSkippable(skippable);

    long stageId = request.getLastStageId() + 1;
    if (0L == stageId) {
      stageId = 1L;
    }
    stage.setStageId(stageId);
    entity.setStageId(Long.valueOf(stageId));

    s_actionExecutionHelper.get().addExecutionCommandsToStage(actionContext, stage, allowRetry);

    // need to set meaningful text on the command
    for (Map<String, HostRoleCommand> map : stage.getHostRoleCommands().values()) {
      for (HostRoleCommand hrc : map.values()) {
        hrc.setCommandDetail(entity.getText());
      }
    }

    request.addStages(Collections.singletonList(stage));
  }

  private void makeRestartStage(UpgradeContext context, RequestStageContainer request,
                                UpgradeItemEntity entity, StageWrapper wrapper,
                                boolean skippable, boolean allowRetry) throws AmbariException {

    Cluster cluster = context.getCluster();

    List<RequestResourceFilter> filters = new ArrayList<RequestResourceFilter>();

    for (TaskWrapper tw : wrapper.getTasks()) {
      // add each host to this stage
      filters.add(new RequestResourceFilter(tw.getService(), tw.getComponent(),
          new ArrayList<String>(tw.getHosts())));
    }

    Map<String, String> restartCommandParams = new HashMap<String, String>();
    restartCommandParams.put(COMMAND_PARAM_RESTART_TYPE, "rolling_upgrade");
    restartCommandParams.put(COMMAND_PARAM_VERSION, context.getVersion());
    restartCommandParams.put(COMMAND_PARAM_DIRECTION, context.getDirection().name().toLowerCase());

    ActionExecutionContext actionContext = new ActionExecutionContext(
        cluster.getClusterName(), "RESTART",
        filters,
        restartCommandParams);
    actionContext.setTimeout(Short.valueOf(s_configuration.getDefaultAgentTaskTimeout(false)));
    actionContext.setIgnoreMaintenance(true);

    ExecuteCommandJson jsons = s_commandExecutionHelper.get().getCommandJson(
        actionContext, cluster);

    Stage stage = s_stageFactory.get().createNew(request.getId().longValue(),
        "/tmp/ambari",
        cluster.getClusterName(),
        cluster.getClusterId(),
        entity.getText(),
        jsons.getClusterHostInfo(),
        jsons.getCommandParamsForStage(),
        jsons.getHostParamsForStage());

    stage.setSkippable(skippable);

    long stageId = request.getLastStageId() + 1;
    if (0L == stageId) {
      stageId = 1L;
    }
    stage.setStageId(stageId);
    entity.setStageId(Long.valueOf(stageId));

    Map<String, String> requestParams = new HashMap<String, String>();
    requestParams.put("command", "RESTART");

    s_commandExecutionHelper.get().addExecutionCommandsToStage(actionContext, stage, requestParams, allowRetry);

    request.addStages(Collections.singletonList(stage));
  }

  private void makeServiceCheckStage(UpgradeContext context, RequestStageContainer request,
                                     UpgradeItemEntity entity, StageWrapper wrapper,
                                     boolean skippable, boolean allowRetry) throws AmbariException {

    List<RequestResourceFilter> filters = new ArrayList<RequestResourceFilter>();

    for (TaskWrapper tw : wrapper.getTasks()) {
      filters.add(new RequestResourceFilter(tw.getService(), "", Collections.<String>emptyList()));
    }

    Cluster cluster = context.getCluster();

    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put(COMMAND_PARAM_VERSION, context.getVersion());
    commandParams.put(COMMAND_PARAM_DIRECTION, context.getDirection().name().toLowerCase());

    ActionExecutionContext actionContext = new ActionExecutionContext(
        cluster.getClusterName(), "SERVICE_CHECK",
        filters,
        commandParams);
    actionContext.setTimeout(Short.valueOf(s_configuration.getDefaultAgentTaskTimeout(false)));
    actionContext.setIgnoreMaintenance(true);

    ExecuteCommandJson jsons = s_commandExecutionHelper.get().getCommandJson(
        actionContext, cluster);

    Stage stage = s_stageFactory.get().createNew(request.getId().longValue(),
        "/tmp/ambari",
        cluster.getClusterName(),
        cluster.getClusterId(),
        entity.getText(),
        jsons.getClusterHostInfo(),
        jsons.getCommandParamsForStage(),
        jsons.getHostParamsForStage());

    stage.setSkippable(skippable);

    long stageId = request.getLastStageId() + 1;
    if (0L == stageId) {
      stageId = 1L;
    }
    stage.setStageId(stageId);
    entity.setStageId(Long.valueOf(stageId));

    Map<String, String> requestParams = new HashMap<String, String>();

    s_commandExecutionHelper.get().addExecutionCommandsToStage(actionContext, stage, requestParams, allowRetry);

    request.addStages(Collections.singletonList(stage));
  }

  private void makeServerSideStage(UpgradeContext context, RequestStageContainer request,
                                   UpgradeItemEntity entity, ServerSideActionTask task,
                                   boolean skippable, boolean allowRtery) throws AmbariException {

    Cluster cluster = context.getCluster();

    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put(COMMAND_PARAM_CLUSTER_NAME, cluster.getClusterName());
    commandParams.put(COMMAND_PARAM_VERSION, context.getVersion());
    commandParams.put(COMMAND_PARAM_DIRECTION, context.getDirection().name().toLowerCase());

    String itemDetail = entity.getText();
    String stageText = StringUtils.abbreviate(entity.getText(), 255);

    switch (task.getType()) {
      case MANUAL: {
        ManualTask mt = (ManualTask) task;
        itemDetail = mt.message;
        if (null != mt.summary) {
          stageText = mt.summary;
        }
        entity.setText(itemDetail);

        if (null != mt.structuredOut) {
          commandParams.put(COMMAND_PARAM_STRUCT_OUT, mt.structuredOut);
        }

        break;
      }
      case CONFIGURE: {
        ConfigureTask ct = (ConfigureTask) task;
        Map<String, String> configProperties = ct.getConfigurationProperties(cluster);

        // if the properties are empty it means that the conditions in the
        // task did not pass;
        if (configProperties.isEmpty()) {
          stageText = "No conditions were met for this configuration task.";
          itemDetail = stageText;
        } else {
          commandParams.putAll(configProperties);

          // extract the config type, key and value to use to build the
          // summary and detail
          String configType = configProperties.get(ConfigureTask.PARAMETER_CONFIG_TYPE);
          String key = configProperties.get(ConfigureTask.PARAMETER_KEY);
          String value = configProperties.get(ConfigureTask.PARAMETER_VALUE);

          itemDetail = String.format("Updating config %s/%s to %s", configType,
              key, value);

          if (null != ct.summary) {
            stageText = ct.summary;
          } else {
            stageText = String.format("Updating Config %s", configType);
          }
        }

        entity.setText(itemDetail);
        break;
      }
      default:
        break;
    }

    ActionExecutionContext actionContext = new ActionExecutionContext(
        cluster.getClusterName(), Role.AMBARI_SERVER_ACTION.toString(),
        Collections.<RequestResourceFilter>emptyList(),
        commandParams);

    actionContext.setTimeout(Short.valueOf((short)-1));
    actionContext.setIgnoreMaintenance(true);

    ExecuteCommandJson jsons = s_commandExecutionHelper.get().getCommandJson(
        actionContext, cluster);

    Stage stage = s_stageFactory.get().createNew(request.getId().longValue(),
        "/tmp/ambari",
        cluster.getClusterName(),
        cluster.getClusterId(),
        stageText,
        jsons.getClusterHostInfo(),
        jsons.getCommandParamsForStage(),
        jsons.getHostParamsForStage());

    stage.setSkippable(skippable);

    long stageId = request.getLastStageId() + 1;
    if (0L == stageId) {
      stageId = 1L;
    }
    stage.setStageId(stageId);
    entity.setStageId(Long.valueOf(stageId));

    // !!! hack hack hack
    String host = cluster.getAllHostsDesiredConfigs().keySet().iterator().next();

    stage.addServerActionCommand(task.getImplementationClass(),
        getManagementController().getAuthName(),
        Role.AMBARI_SERVER_ACTION,
        RoleCommand.EXECUTE,
        cluster.getClusterName(),
        host,
        new ServiceComponentHostServerActionEvent(StageUtils.getHostName(), System.currentTimeMillis()),
        commandParams,
        itemDetail,
        null,
        Integer.valueOf(1200),
        allowRtery);

    request.addStages(Collections.singletonList(stage));
  }
}
