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

import org.apache.ambari.server.AmbariException;
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
import org.apache.ambari.server.orm.entities.UpgradeItemEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.ambari.server.state.stack.UpgradePack.ProcessingComponent;
import org.apache.ambari.server.state.stack.upgrade.Task;
import org.apache.ambari.server.utils.StageUtils;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Manages the ability to start and get status of upgrades.
 */
@StaticallyInject
public class UpgradeResourceProvider extends AbstractControllerResourceProvider {

  protected static final String UPGRADE_ID = "Upgrade/id";
  protected static final String UPGRADE_CLUSTER_NAME = "Upgrade/cluster_name";
  protected static final String UPGRADE_VERSION = "Upgrade/version";

  private static final Set<String> PK_PROPERTY_IDS = new HashSet<String>(
      Arrays.asList(UPGRADE_ID, UPGRADE_CLUSTER_NAME));
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



  static {
    // properties
    PROPERTY_IDS.add(UPGRADE_ID);
    PROPERTY_IDS.add(UPGRADE_CLUSTER_NAME);
    PROPERTY_IDS.add(UPGRADE_VERSION);

    // keys
    KEY_PROPERTY_IDS.put(Resource.Type.Upgrade, UPGRADE_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.Cluster, UPGRADE_CLUSTER_NAME);
  }

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

    for (final Map<String, Object> requestMap : requestMaps) {
      createResources(new Command<Void>() {
        @Override
        public Void invoke() throws AmbariException {
          UpgradePack up = validateRequest(requestMap);

          createUpgrade(up, requestMap);

          return null;
        };
      });
    }

    notifyCreate(Resource.Type.Upgrade, request);
    return getRequestStatus(null);
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

      List<UpgradeEntity> upgrades = m_upgradeDAO.findUpgrades(cluster.getClusterId());

      for (UpgradeEntity entity : upgrades) {
        results.add(toResource(entity, clusterName, requestPropertyIds));
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

    setResourceProperty(resource, UPGRADE_ID, entity.getId(), requestedIds);
    setResourceProperty(resource, UPGRADE_CLUSTER_NAME, clusterName, requestedIds);

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

  private UpgradeEntity createUpgrade(UpgradePack pack, Map<String, Object> requestMap)
    throws AmbariException {

    String clusterName = (String) requestMap.get(UPGRADE_CLUSTER_NAME);

    if (null == clusterName) {
      throw new AmbariException(String.format("%s is required", UPGRADE_CLUSTER_NAME));
    }

    Cluster cluster = getManagementController().getClusters().getCluster(clusterName);
    Map<String, Service> clusterServices = cluster.getServices();

    Map<String, Map<String, ProcessingComponent>> tasks = pack.getTasks();

    List<StageHolder> preUpgrades = new ArrayList<StageHolder>();
    List<StageHolder> restart = new ArrayList<StageHolder>();
    List<StageHolder> postUpgrades = new ArrayList<StageHolder>();

    for (Entry<String, List<String>> entry : pack.getOrder().entrySet()) {
      String serviceName = entry.getKey();
      List<String> componentNames = entry.getValue();

      // !!! if the upgrade pack doesn't define any tasks, skip
      if (!tasks.containsKey(serviceName)) {
        continue;
      }

      // !!! if the service isn't installed, skip
      if (!clusterServices.containsKey(serviceName)) {
        continue;
      }

      Service service = clusterServices.get(serviceName);
      Map<String, ServiceComponent> components = service.getServiceComponents();

      for (String componentName : componentNames) {
        // !!! if the upgrade pack has no tasks for component, skip
        if (!tasks.get(serviceName).containsKey(componentName)) {
          continue;
        }

        // !!! if the component is not installed with the cluster, skip
        if (!components.containsKey(componentName)) {
          continue;
        }

        ProcessingComponent pc = tasks.get(serviceName).get(componentName);

        List<Set<String>> groupings = computeHostGroupings(pc,
            components.get(componentName).getServiceComponentHosts().keySet());

        preUpgrades.addAll(buildUpgradeStages(pc, true, groupings));
        restart.addAll(buildRollingRestart(serviceName, pc, groupings));
        postUpgrades.addAll(buildUpgradeStages(pc, false, groupings));
      }
    }

    Gson gson = new Gson();

    UpgradeEntity entity = new UpgradeEntity();

    List<UpgradeItemEntity> items = new ArrayList<UpgradeItemEntity>();
    for (StageHolder holder : preUpgrades) {
      holder.upgradeItemEntity.setHosts(gson.toJson(holder.hosts));
      holder.upgradeItemEntity.setTasks(gson.toJson(holder.taskHolder.tasks));
      items.add(holder.upgradeItemEntity);
    }

    for (StageHolder holder : restart) {
      holder.upgradeItemEntity.setHosts(gson.toJson(holder.hosts));
      items.add(holder.upgradeItemEntity);
    }

    for (StageHolder holder : postUpgrades) {
      holder.upgradeItemEntity.setHosts(gson.toJson(holder.hosts));
      holder.upgradeItemEntity.setTasks(gson.toJson(holder.taskHolder.tasks));
      items.add(holder.upgradeItemEntity);
    }

    entity.setClusterId(Long.valueOf(cluster.getClusterId()));
    entity.setUpgradeItems(items);

    m_upgradeDAO.create(entity);

    RequestStageContainer req = createRequest((String) requestMap.get(UPGRADE_VERSION));

    for (StageHolder holder : preUpgrades) {
      createUpgradeTaskStage(cluster, req, holder);
    }

    for (StageHolder holder : restart) {
      createRestartStage(cluster, req, holder);
    }

    for (StageHolder holder : postUpgrades) {
      createUpgradeTaskStage(cluster, req, holder);
    }

    req.getRequestStatusResponse();

    req.persist();

    return entity;
  }

  private List<StageHolder> buildUpgradeStages(ProcessingComponent pc,
      boolean preUpgrade, List<Set<String>> hostGroups) {

    List<TaskHolder> taskHolders = buildStageStrategy(
        preUpgrade ? pc.preTasks : pc.postTasks);

    List<StageHolder> stages = new ArrayList<StageHolder>();

    StringBuilder sb = new StringBuilder(preUpgrade ? "Preparing " : "Finalizing ");
    sb.append("%s on %d host(s).  Phase %s/%s");
    String textFormat = sb.toString();

    for (TaskHolder taskHolder : taskHolders) {
      int i = 1;
      for (Set<String> hostGroup : hostGroups) {
        StageHolder stage = new StageHolder();
        stage.hosts = hostGroup;
        stage.taskHolder = taskHolder;
        stage.upgradeItemEntity = new UpgradeItemEntity();
        stage.upgradeItemEntity.setText(String.format(textFormat,
            pc.name,
            Integer.valueOf(hostGroup.size()),
            Integer.valueOf(i++),
            Integer.valueOf(hostGroups.size())));
        stages.add(stage);
      }
    }

    return stages;
  }

  /**
   * Builds the stages for the rolling restart portion
   * @param pc the information from the upgrade pack
   * @param hostGroups a list of the host groupings
   * @return the list of stages that need to be created
   */
  private List<StageHolder> buildRollingRestart(String serviceName, ProcessingComponent pc,
      List<Set<String>> hostGroups) {
    List<StageHolder> stages = new ArrayList<StageHolder>();

    String textFormat = "Restarting %s on %d host(s), Phase %d/%d";

    int i = 1;
    for (Set<String> hostGroup : hostGroups) {
      // !!! each of these is its own stage
      StageHolder stage = new StageHolder();
      stage.service = serviceName;
      stage.component = pc.name;
      stage.hosts = hostGroup;
      stage.upgradeItemEntity = new UpgradeItemEntity();
      stage.upgradeItemEntity.setText(String.format(textFormat, pc.name,
          Integer.valueOf(hostGroup.size()),
          Integer.valueOf(i++),
          Integer.valueOf(hostGroups.size())));
      stages.add(stage);
    }

    return stages;
  }


  /**
   * Calculates how the hosts will be executing their upgrades.
   */
  private List<Set<String>> computeHostGroupings(ProcessingComponent taskBuckets, Set<String> allHosts) {
    if (null == taskBuckets.batch) {
      return Collections.singletonList(allHosts);
    } else {
      return taskBuckets.batch.getHostGroupings(allHosts);
    }
  }

  /**
   * For all the tasks for a component, separate out the manual from the
   * automated steps into the stages they should executed.
   *
   * @param tasks a list of tasks
   * @return the list of stages
   */
  private List<TaskHolder> buildStageStrategy(List<Task> tasks) {
    if (null == tasks)
      return Collections.emptyList();

    List<TaskHolder> holders = new ArrayList<TaskHolder>();
    TaskHolder holder = new TaskHolder();

    holders.add(holder);
    int i = 0;
    for (Task t : tasks) {
      // !!! TODO should every manual task get its own stage?
      if (i > 0 && t.getType().isManual() != tasks.get(i-1).getType().isManual()) {
        holder = new TaskHolder();
        holders.add(holder);
      }

      holder.tasks.add(t);
      i++;
    }

    return holders;
  }

  private static class TaskHolder {
    private List<Task> tasks = new ArrayList<Task>();
  }

  private static class StageHolder {
    private String service;
    private String component;
    private TaskHolder taskHolder;
    private UpgradeItemEntity upgradeItemEntity;
    private Set<String> hosts;
  }


  private RequestStageContainer createRequest(String version) {
    ActionManager actionManager = getManagementController().getActionManager();

    RequestStageContainer requestStages = new RequestStageContainer(
        actionManager.getNextRequestId(), null, requestFactory.get(), actionManager);
    requestStages.setRequestContext(String.format("Upgrading to %s", version));

    return requestStages;
  }

  /**
   * Creates a stage and appends it to the request.
   * @param cluster the cluster
   * @param request the request container
   * @param holder the holder
   * @throws AmbariException
   */
  private void createUpgradeTaskStage(Cluster cluster, RequestStageContainer request,
      StageHolder holder) throws AmbariException {

    Map<String, String> hostLevelParams = new HashMap<String, String>();
    hostLevelParams.put(JDK_LOCATION, getManagementController().getJdkResourceUrl());

    Stage stage = stageFactory.get().createNew(request.getId().longValue(),
        "/tmp/ambari",
        cluster.getClusterName(),
        cluster.getClusterId(),
        holder.upgradeItemEntity.getText(),
        "{}", "{}",
        StageUtils.getGson().toJson(hostLevelParams));

    long stageId = request.getLastStageId() + 1;
    if (0L == stageId) {
      stageId = 1L;
    }
    stage.setStageId(stageId);

    // add each host to this stage
    RequestResourceFilter filter = new RequestResourceFilter("", "",
        new ArrayList<String>(holder.hosts));

    // !!! TODO when the custom action is underway, change this
    Map<String, String> params = new HashMap<String, String>();
    params.put("tasks", "TheTaskInfo");

    ActionExecutionContext actionContext = new ActionExecutionContext(
        cluster.getClusterName(), "ru_execute_tasks",
        Collections.singletonList(filter),
        params);
    actionContext.setTimeout(Short.valueOf((short)60));

    // !!! TODO verify the action is valid

    actionExecutionHelper.get().addExecutionCommandsToStage(actionContext, stage);

    // need to set meaningful text on the command
    for (Map<String, HostRoleCommand> map : stage.getHostRoleCommands().values()) {
      for (HostRoleCommand hrc : map.values()) {
        hrc.setCommandDetail(holder.upgradeItemEntity.getText());
      }
    }

    request.addStages(Collections.singletonList(stage));
  }

  private void createRestartStage(Cluster cluster, RequestStageContainer request,
      StageHolder holder) throws AmbariException {

    // add each host to this stage
    RequestResourceFilter filter = new RequestResourceFilter(holder.service, holder.component,
        new ArrayList<String>(holder.hosts));

    ActionExecutionContext actionContext = new ActionExecutionContext(
        cluster.getClusterName(), "RESTART",
        Collections.singletonList(filter),
        Collections.<String, String>emptyMap());
    actionContext.setTimeout(Short.valueOf((short)-1));

    ExecuteCommandJson jsons = commandExecutionHelper.get().getCommandJson(
        actionContext, cluster);

    Stage stage = stageFactory.get().createNew(request.getId().longValue(),
        "/tmp/ambari",
        cluster.getClusterName(),
        cluster.getClusterId(),
        holder.upgradeItemEntity.getText(),
        jsons.getClusterHostInfo(),
        jsons.getCommandParamsForStage(),
        jsons.getHostParamsForStage());

    long stageId = request.getLastStageId() + 1;
    if (0L == stageId) {
      stageId = 1L;
    }
    stage.setStageId(stageId);

    // !!! TODO verify the action is valid

    Map<String, String> requestParams = new HashMap<String, String>();
    requestParams.put("command", "RESTART");

    commandExecutionHelper.get().addExecutionCommandsToStage(actionContext, stage, requestParams);

    request.addStages(Collections.singletonList(stage));
  }

}
