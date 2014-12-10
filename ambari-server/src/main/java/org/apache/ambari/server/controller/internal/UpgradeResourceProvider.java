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
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
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
import org.apache.ambari.server.serveraction.upgrades.ManualStageAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.UpgradeHelper;
import org.apache.ambari.server.state.UpgradeHelper.UpgradeGroupHolder;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.ambari.server.state.stack.upgrade.StageWrapper;
import org.apache.ambari.server.state.stack.upgrade.TaskWrapper;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostServerActionEvent;
import org.apache.ambari.server.utils.StageUtils;
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

  private static final Set<String> PK_PROPERTY_IDS = new HashSet<String>(
      Arrays.asList(UPGRADE_REQUEST_ID, UPGRADE_CLUSTER_NAME));
  private static final Set<String> PROPERTY_IDS = new HashSet<String>();

  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = new HashMap<Resource.Type, String>();

  @Inject
  private static UpgradeDAO m_upgradeDAO = null;
  @Inject
  private static Provider<AmbariMetaInfo> m_metaProvider = null;
  @Inject
  private static RepositoryVersionDAO m_repoVersionDAO = null;
  @Inject
  private static Provider<RequestFactory> requestFactory;
  @Inject
  private static Provider<StageFactory> stageFactory;
  @Inject
  private static Provider<AmbariActionExecutionHelper> actionExecutionHelper;
  @Inject
  private static Provider<AmbariCustomCommandExecutionHelper> commandExecutionHelper;

  private static Map<String, String> REQUEST_PROPERTY_MAP = new HashMap<String, String>();

  static {
    // properties
    PROPERTY_IDS.add(UPGRADE_CLUSTER_NAME);
    PROPERTY_IDS.add(UPGRADE_VERSION);
    PROPERTY_IDS.add(UPGRADE_REQUEST_ID);

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
   * @param controller
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

    UpgradeEntity entity = createResources(new Command<UpgradeEntity>() {
        @Override
        public UpgradeEntity invoke() throws AmbariException {
          UpgradePack up = validateRequest(requestMap);

          return createUpgrade(up, requestMap);
        };
      });

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

      Cluster cluster = null;
      try {
        cluster = getManagementController().getClusters().getCluster(clusterName);
      } catch (AmbariException e) {
        throw new NoSuchResourceException(String.format("Cluster %s could not be loaded", clusterName));
      }

      List<UpgradeEntity> upgrades = new ArrayList<UpgradeEntity>();

      String upgradeIdStr = (String) propertyMap.get(UPGRADE_REQUEST_ID);
      if (null != upgradeIdStr) {
        UpgradeEntity upgrade = m_upgradeDAO.findUpgradeByRequestId(Long.valueOf(upgradeIdStr));

        if (null != upgrade) {
          upgrades.add(upgrade);
        }
      } else {
        upgrades = m_upgradeDAO.findUpgrades(cluster.getClusterId());
      }

      UpgradeHelper helper = new UpgradeHelper();
      for (UpgradeEntity entity : upgrades) {
        Resource r = toResource(entity, clusterName, requestPropertyIds);
        results.add(r);

        // !!! not terribly efficient, but that's ok in this case.  The handful-per-year
        // an upgrade is done won't kill performance.
        Resource r1 = helper.getRequestResource(clusterName, entity.getRequestId());
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

    modifyResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        return null;
      }
    });

    notifyUpdate(Resource.Type.Upgrade, request, predicate);

    return getRequestStatus(null);
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
    throw new SystemException("Cannot delete upgrades");
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

    return resource;
  }

  /**
   * Validates a singular API request.
   *
   * @param requestMap the map of properties
   * @return the validated upgrade pack
   * @throws AmbariException
   */
  private UpgradePack validateRequest(Map<String, Object> requestMap) throws AmbariException {
    String clusterName = (String) requestMap.get(UPGRADE_CLUSTER_NAME);
    String version = (String) requestMap.get(UPGRADE_VERSION);

    if (null == clusterName) {
      throw new AmbariException(String.format("%s is required", UPGRADE_CLUSTER_NAME));
    }

    if (null == version) {
      throw new AmbariException(String.format("%s is required", UPGRADE_VERSION));
    }

    Cluster cluster = getManagementController().getClusters().getCluster(clusterName);
    StackId stack = cluster.getDesiredStackVersion();
    RepositoryVersionEntity versionEntity = m_repoVersionDAO.findByStackAndVersion(
        stack.getStackId(), version);

    if (null == versionEntity) {
      throw new AmbariException(String.format("Version %s for stack %s was not found",
          version, stack.getStackVersion()));
    }

    Map<String, UpgradePack> packs = m_metaProvider.get().getUpgradePacks(
        stack.getStackName(), stack.getStackVersion());

    UpgradePack up = packs.get(versionEntity.getUpgradePackage());

    if (null == up) {
      throw new AmbariException(String.format(
          "Upgrade pack %s not found", versionEntity.getUpgradePackage()));
    }

    // !!! validate all hosts have the version installed

    return up;
  }

  /**
   * Inject variables into the list of {@link org.apache.ambari.server.orm.entities.UpgradeItemEntity} items, whose
   * tasks may use strings like {{configType/propertyName}} that need to be retrieved from the properties.
   * @param configHelper Configuration Helper
   * @param cluster Cluster
   * @param items Collection of items whose tasks will be injected.
   * @return Return the collection of items with the injected properties.
   */
  private List<UpgradeItemEntity> injectVariables(ConfigHelper configHelper, Cluster cluster, List<UpgradeItemEntity> items) {
    final String regexp = "(\\{\\{.*?\\}\\})";

    for (UpgradeItemEntity upgradeItem : items) {
      String task = upgradeItem.getTasks();
      if (task != null && !task.isEmpty()) {
        Matcher m = Pattern.compile(regexp).matcher(task);
        while(m.find()) {
          String origVar = m.group(1);
          String formattedVar = origVar.substring(2, origVar.length() - 2).trim();

          int posConfigFile = formattedVar.indexOf("/");
          if (posConfigFile > 0) {
            String configType = formattedVar.substring(0, posConfigFile);
            String propertyName = formattedVar.substring(posConfigFile + 1, formattedVar.length());
            try {
              // TODO, some properties use 0.0.0.0 to indicate the current host.
              // Right now, ru_execute_tasks.py is responsible for replacing 0.0.0.0 with the hostname.

              String configValue = configHelper.getPropertyValueFromStackDefinitions(cluster, configType, propertyName);
              task = task.replace(origVar, configValue);
            } catch (Exception err) {
              LOG.error(String.format("Exception trying to retrieve property %s/%s. Error: %s", configType, propertyName, err.getMessage()));
            }
          }
        }
        upgradeItem.setTasks(task);
      }
    }
    return items;
  }

  private UpgradeEntity createUpgrade(UpgradePack pack, Map<String, Object> requestMap)
    throws AmbariException {

    String clusterName = (String) requestMap.get(UPGRADE_CLUSTER_NAME);

    if (null == clusterName) {
      throw new AmbariException(String.format("%s is required", UPGRADE_CLUSTER_NAME));
    }

    Cluster cluster = getManagementController().getClusters().getCluster(clusterName);
    ConfigHelper configHelper = getManagementController().getConfigHelper();

    UpgradeHelper helper = new UpgradeHelper();
    List<UpgradeGroupHolder> groups = helper.createUpgrade(cluster, pack);
    List<UpgradeGroupEntity> groupEntities = new ArrayList<UpgradeGroupEntity>();

    final String version = (String) requestMap.get(UPGRADE_VERSION);
    RequestStageContainer req = createRequest(version);

    for (UpgradeGroupHolder group : groups) {
      UpgradeGroupEntity groupEntity = new UpgradeGroupEntity();
      groupEntity.setName(group.name);
      groupEntity.setTitle(group.title);

      List<UpgradeItemEntity> itemEntities = new ArrayList<UpgradeItemEntity>();

      for (StageWrapper wrapper : group.items) {
        UpgradeItemEntity itemEntity = new UpgradeItemEntity();
        itemEntity.setText(wrapper.getText());
        itemEntity.setTasks(wrapper.getTasksJson());
        itemEntity.setHosts(wrapper.getHostsJson());
        itemEntities.add(itemEntity);

        // upgrade items match a stage
        createStage(cluster, req, version, itemEntity, wrapper);
      }

      itemEntities = injectVariables(configHelper, cluster, itemEntities);

      groupEntity.setItems(itemEntities);

      groupEntities.add(groupEntity);

    }

    UpgradeEntity entity = new UpgradeEntity();
    entity.setUpgradeGroups(groupEntities);
    entity.setClusterId(Long.valueOf(cluster.getClusterId()));

    req.getRequestStatusResponse();

    entity.setRequestId(req.getId());

    req.persist();

    m_upgradeDAO.create(entity);

    return entity;
  }


  private RequestStageContainer createRequest(String version) {
    ActionManager actionManager = getManagementController().getActionManager();

    RequestStageContainer requestStages = new RequestStageContainer(
        actionManager.getNextRequestId(), null, requestFactory.get(), actionManager);
    requestStages.setRequestContext(String.format("Upgrading to %s", version));

    return requestStages;
  }

  private void createStage(Cluster cluster, RequestStageContainer request, final String version,
      UpgradeItemEntity entity, StageWrapper wrapper) throws AmbariException {

    switch (wrapper.getType()) {
      case RESTART:
        makeRestartStage(cluster, request, version, entity, wrapper);
        break;
      case RU_TASKS:
        makeActionStage(cluster, request, version, entity, wrapper);
        break;
      case SERVICE_CHECK:
        makeServiceCheckStage(cluster, request, version, entity, wrapper);
        break;
      case MANUAL:
        makeManualStage(cluster, request, version, entity, wrapper);
        break;
    }

  }

  private void makeActionStage(Cluster cluster, RequestStageContainer request, final String version,
      UpgradeItemEntity entity, StageWrapper wrapper) throws AmbariException {

    // add each host to this stage
    RequestResourceFilter filter = new RequestResourceFilter("", "",
        new ArrayList<String>(wrapper.getHosts()));

    Map<String, String> params = new HashMap<String, String>();
    params.put("tasks", entity.getTasks());

    ActionExecutionContext actionContext = new ActionExecutionContext(
        cluster.getClusterName(), "ru_execute_tasks",
        Collections.singletonList(filter),
        params);
    actionContext.setTimeout(Short.valueOf((short)60));

    Map<String, String> hostLevelParams = new HashMap<String, String>();
    hostLevelParams.put(JDK_LOCATION, getManagementController().getJdkResourceUrl());

    Stage stage = stageFactory.get().createNew(request.getId().longValue(),
        "/tmp/ambari",
        cluster.getClusterName(),
        cluster.getClusterId(),
        entity.getText(),
        "{}", "{}",
        StageUtils.getGson().toJson(hostLevelParams));

    long stageId = request.getLastStageId() + 1;
    if (0L == stageId) {
      stageId = 1L;
    }
    stage.setStageId(stageId);
    entity.setStageId(Long.valueOf(stageId));

    // !!! TODO verify the action is valid

    actionExecutionHelper.get().addExecutionCommandsToStage(actionContext, stage);

    // need to set meaningful text on the command
    for (Map<String, HostRoleCommand> map : stage.getHostRoleCommands().values()) {
      for (HostRoleCommand hrc : map.values()) {
        hrc.setCommandDetail(entity.getText());
      }
    }

    request.addStages(Collections.singletonList(stage));
  }

  private void makeRestartStage(Cluster cluster, RequestStageContainer request, final String version,
      UpgradeItemEntity entity, StageWrapper wrapper) throws AmbariException {

    List<RequestResourceFilter> filters = new ArrayList<RequestResourceFilter>();

    for (TaskWrapper tw : wrapper.getTasks()) {
      // add each host to this stage
      filters.add(new RequestResourceFilter(tw.getService(), tw.getComponent(),
          new ArrayList<String>(tw.getHosts())));
    }

    Map<String, String> restartCommandParams = new HashMap<String, String>();
    restartCommandParams.put("restart_type", "rolling_upgrade");
    restartCommandParams.put("version", version);

    ActionExecutionContext actionContext = new ActionExecutionContext(
        cluster.getClusterName(), "RESTART",
        filters,
        restartCommandParams);
    actionContext.setTimeout(Short.valueOf((short)-1));

    ExecuteCommandJson jsons = commandExecutionHelper.get().getCommandJson(
        actionContext, cluster);

    Stage stage = stageFactory.get().createNew(request.getId().longValue(),
        "/tmp/ambari",
        cluster.getClusterName(),
        cluster.getClusterId(),
        entity.getText(),
        jsons.getClusterHostInfo(),
        jsons.getCommandParamsForStage(),
        jsons.getHostParamsForStage());

    long stageId = request.getLastStageId() + 1;
    if (0L == stageId) {
      stageId = 1L;
    }
    stage.setStageId(stageId);
    entity.setStageId(Long.valueOf(stageId));

    // !!! TODO verify the action is valid

    Map<String, String> requestParams = new HashMap<String, String>();
    requestParams.put("command", "RESTART");

    commandExecutionHelper.get().addExecutionCommandsToStage(actionContext, stage, requestParams);

    request.addStages(Collections.singletonList(stage));
  }

  private void makeServiceCheckStage(Cluster cluster, RequestStageContainer request, String version,
      UpgradeItemEntity entity, StageWrapper wrapper) throws AmbariException {

    List<RequestResourceFilter> filters = new ArrayList<RequestResourceFilter>();

    for (TaskWrapper tw : wrapper.getTasks()) {
      filters.add(new RequestResourceFilter(tw.getService(), "", Collections.<String>emptyList()));
    }

    Map<String, String> restartCommandParams = new HashMap<String, String>();
    restartCommandParams.put("version", version);

    ActionExecutionContext actionContext = new ActionExecutionContext(
        cluster.getClusterName(), "SERVICE_CHECK",
        filters,
        restartCommandParams);
    actionContext.setTimeout(Short.valueOf((short)-1));

    ExecuteCommandJson jsons = commandExecutionHelper.get().getCommandJson(
        actionContext, cluster);

    Stage stage = stageFactory.get().createNew(request.getId().longValue(),
        "/tmp/ambari",
        cluster.getClusterName(),
        cluster.getClusterId(),
        entity.getText(),
        jsons.getClusterHostInfo(),
        jsons.getCommandParamsForStage(),
        jsons.getHostParamsForStage());

    long stageId = request.getLastStageId() + 1;
    if (0L == stageId) {
      stageId = 1L;
    }
    stage.setStageId(stageId);
    entity.setStageId(Long.valueOf(stageId));

    Map<String, String> requestParams = new HashMap<String, String>();

    commandExecutionHelper.get().addExecutionCommandsToStage(actionContext, stage, requestParams);

    request.addStages(Collections.singletonList(stage));
  }

  private void makeManualStage(Cluster cluster, RequestStageContainer request, String version,
      UpgradeItemEntity entity, StageWrapper wrapper) throws AmbariException {

    Map<String, String> restartCommandParams = new HashMap<String, String>();
    restartCommandParams.put("version", version);

    ActionExecutionContext actionContext = new ActionExecutionContext(
        cluster.getClusterName(), Role.AMBARI_SERVER_ACTION.toString(),
        Collections.<RequestResourceFilter>emptyList(),
        restartCommandParams);
    actionContext.setTimeout(Short.valueOf((short)-1));

    ExecuteCommandJson jsons = commandExecutionHelper.get().getCommandJson(
        actionContext, cluster);


    Stage stage = stageFactory.get().createNew(request.getId().longValue(),
        "/tmp/ambari",
        cluster.getClusterName(),
        cluster.getClusterId(),
        entity.getText(),
        jsons.getClusterHostInfo(),
        jsons.getCommandParamsForStage(),
        jsons.getHostParamsForStage());

    long stageId = request.getLastStageId() + 1;
    if (0L == stageId) {
      stageId = 1L;
    }
    stage.setStageId(stageId);
    entity.setStageId(Long.valueOf(stageId));

    // !!! hack hack hack
    String host = cluster.getAllHostsDesiredConfigs().keySet().iterator().next();

    stage.addServerActionCommand(ManualStageAction.class.getName(),
        Role.AMBARI_SERVER_ACTION,
        RoleCommand.EXECUTE,
        cluster.getClusterName(), host,
        new ServiceComponentHostServerActionEvent(StageUtils.getHostName(), System.currentTimeMillis()),
        Collections.<String, String>emptyMap(), 1200);

    request.addStages(Collections.singletonList(stage));

  }


}
