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
package org.apache.ambari.server.controller.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
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
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.MpackDAO;
import org.apache.ambari.server.orm.dao.MpackHostStateDAO;
import org.apache.ambari.server.orm.dao.ServiceGroupDAO;
import org.apache.ambari.server.orm.dao.UpgradePlanDAO;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.MpackEntity;
import org.apache.ambari.server.orm.entities.MpackHostStateEntity;
import org.apache.ambari.server.orm.entities.RepoOsEntity;
import org.apache.ambari.server.orm.entities.ServiceGroupEntity;
import org.apache.ambari.server.orm.entities.UpgradePlanDetailEntity;
import org.apache.ambari.server.orm.entities.UpgradePlanEntity;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Mpack;
import org.apache.ambari.server.state.MpackInstallState;
import org.apache.ambari.server.state.OsSpecific;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.stack.upgrade.RepositoryVersionHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;

/**
 * Manages upgrade plans.
 */
@StaticallyInject
@Experimental(feature=ExperimentalFeature.UNIT_TEST_REQUIRED)
public class UpgradePlanInstallResourceProvider extends AbstractControllerResourceProvider {

  private static final String UPGRADE_PLAN_INSTALL = "UpgradePlanInstall" + PropertyHelper.EXTERNAL_PATH_SEP;

  protected static final String UPGRADE_PLAN_INSTALL_ID             = UPGRADE_PLAN_INSTALL + "id";
  protected static final String UPGRADE_PLAN_INSTALL_CLUSTER_NAME   = UPGRADE_PLAN_INSTALL + "cluster_name";
  protected static final String UPGRADE_PLAN_INSTALL_SUCCESS_FACTOR = UPGRADE_PLAN_INSTALL + "success_factor";
  protected static final String UPGRADE_PLAN_INSTALL_FORCE          = UPGRADE_PLAN_INSTALL + "force";

  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = ImmutableMap.<Resource.Type, String>builder()
      .put(Resource.Type.UpgradePlanInstall, UPGRADE_PLAN_INSTALL_ID)
      .put(Resource.Type.Cluster, UPGRADE_PLAN_INSTALL_CLUSTER_NAME)
      .build();

  private static final Set<String> PK_PROPERTY_IDS = Sets.newHashSet(KEY_PROPERTY_IDS.values());

  private static final Set<String> PROPERTY_IDS = Sets.newHashSet(
      UPGRADE_PLAN_INSTALL_ID,
      UPGRADE_PLAN_INSTALL_CLUSTER_NAME);

  protected static final String MPACK_PACKAGES_ACTION = "mpack_packages";
  protected static final String INSTALL_PACKAGES = "Install Version";

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
  private static final float DEFAULT_SUCCESS_FACTOR = 0.85f;

  /**
   * Used to deserialize the repository JSON into an object.
   */
  @Inject
  private static Gson s_gson;

  @Inject
  private static UpgradePlanDAO s_upgradePlanDAO;

  @Inject
  private static ServiceGroupDAO s_serviceGroupDAO;

  @Inject
  private static MpackDAO s_mpackDAO;

  @Inject
  private static MpackHostStateDAO s_mpackHostStateDAO;

  @Inject
  private static RequestFactory s_requestFactory;

  @Inject
  private static StageFactory s_stageFactory;

  @Inject
  private static Configuration s_configuration;

  @Inject
  private static RepositoryVersionHelper s_repoHelper;

  @Inject
  private static Provider<AmbariActionExecutionHelper> s_actionExecutionHelper;


  /**
   * Constructor.
   *
   * @param controller the controller
   */
  UpgradePlanInstallResourceProvider(AmbariManagementController controller) {
    super(Resource.Type.UpgradePlanInstall, PROPERTY_IDS, KEY_PROPERTY_IDS, controller);

    setRequiredCreateAuthorizations(EnumSet.of(RoleAuthorization.CLUSTER_UPGRADE_DOWNGRADE_STACK));
  }

  @Override
  public RequestStatus createResourcesAuthorized(final Request request)
      throws SystemException,
      UnsupportedPropertyException, ResourceAlreadyExistsException,
      NoSuchParentResourceException {

    throw new SystemException("Upgrade plan installs cannot be created.  Update the resource instead.");
  }

  @Override
  public Set<Resource> getResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    throw new SystemException("Upgrade plan installs cannot be requested");
  }

  @Override
  public RequestStatus updateResourcesAuthorized(final Request request,
      Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);

    if (propertyMaps.isEmpty()) {
      throw new IllegalArgumentException("At least one upgrade plan should be specified.");
    } else if (propertyMaps.size() > 1) {
      throw new IllegalArgumentException("Only one upgrade plan may be installed at one time.");
    }

    // !!! The Highlander Principle: in the end, there can be only one
    Map<String, Object> propertyMap = propertyMaps.iterator().next();

    if (!propertyMap.containsKey(UPGRADE_PLAN_INSTALL_CLUSTER_NAME)) {
      throw new IllegalArgumentException("Cluster name is not specified");
    } else if (!propertyMap.containsKey(UPGRADE_PLAN_INSTALL_ID)) {
      throw new IllegalArgumentException("Upgrade plan id is required");
    }


    String clusterName = propertyMap.get(UPGRADE_PLAN_INSTALL_CLUSTER_NAME).toString();
    Cluster cluster = null;
    try {
      cluster = getManagementController().getClusters().getCluster(clusterName);
    } catch (AmbariException e) {
      throw new IllegalArgumentException(e);
    }

    String upgradePlanIdString = propertyMap.get(UPGRADE_PLAN_INSTALL_ID).toString();
    Long upgradePlanId = NumberUtils.toLong(upgradePlanIdString, -1L);
    if (-1L == upgradePlanId) {
      throw new IllegalArgumentException("Upgrade plan id was specified, but is not a long");
    }

    UpgradePlanEntity upgradePlan = s_upgradePlanDAO.findByPK(upgradePlanId);
    if (null == upgradePlan) {
      throw new IllegalArgumentException(String.format("Upgrade plan %s was not found", upgradePlanId));
    }

    boolean forceAll = false;
    if (propertyMap.containsKey(UPGRADE_PLAN_INSTALL_FORCE)) {
      String forceAllString = propertyMap.get(UPGRADE_PLAN_INSTALL_FORCE).toString();
      forceAll = BooleanUtils.toBoolean(forceAllString);
    }

    Float successFactor = DEFAULT_SUCCESS_FACTOR;
    if (propertyMap.containsKey(UPGRADE_PLAN_INSTALL_SUCCESS_FACTOR)) {
      String successFactorString = propertyMap.get(UPGRADE_PLAN_INSTALL_SUCCESS_FACTOR).toString();
      successFactor = NumberUtils.toFloat(successFactorString, DEFAULT_SUCCESS_FACTOR);
    }

    RequestStageContainer installRequest = null;
    try {
      installRequest = createOrchestration(cluster, upgradePlan, successFactor, forceAll);
    } catch (AmbariException e) {
      throw new IllegalArgumentException(e);
    }

    RequestStatusResponse response = installRequest.getRequestStatusResponse();

    return getRequestStatus(response);
  }

  @Transactional(rollbackOn= {AmbariException.class, RuntimeException.class})
  RequestStageContainer createOrchestration(Cluster cluster, UpgradePlanEntity upgradePlan,
      float successFactor, boolean forceInstall) throws AmbariException, SystemException {

    // TODO - API calls (elsewhere) that make checks for compatible mpacks and selected SGs

    // for each plan detail, the mpack and selected SG has has already been
    // predetermined via compatibility

    // !!! a single host may get more than one mpack to install.  those are different
    // !!! host name -> set of mpack package names
    Map<HostEntity, Set<MpackInstallDetail>> details = new HashMap<>();

    for (UpgradePlanDetailEntity upgradePlanEntity : upgradePlan.getDetails()) {
      long serviceGroupId = upgradePlanEntity.getServiceGroupId();
      ServiceGroupEntity serviceGroup = s_serviceGroupDAO.findByPK(serviceGroupId);

      // is this even possible?
      if (null == serviceGroup.getStack().getMpackId()) {
        continue;
      }

      MpackEntity targetMpackEntity = s_mpackDAO.findById(upgradePlanEntity.getMpackTargetId());
      StackInfo targetStack = getManagementController().getAmbariMetaInfo()
          .getStack(targetMpackEntity.getStackId());

      // find all hosts for the service group via its mpack
      List<MpackHostStateEntity> mpackHosts = s_mpackHostStateDAO.findByMpackAndInstallState(
          serviceGroup.getStack().getMpackId(), MpackInstallState.INSTALLED);

      if (CollectionUtils.isEmpty(mpackHosts)) {
        throw new SystemException("Cannot install upgrade plan as the current mpack is not installed.");
      }

      mpackHosts.forEach(mpackHostStateEntity ->
        {
          Host host = cluster.getHost(mpackHostStateEntity.getHostName());
          HostEntity hostEntity = mpackHostStateEntity.getHostEntity();
          String osFamily = host.getOsFamily();

          List<OsSpecific.Package> packages = new ArrayList<>();

          OsSpecific anyPackages = targetStack.getOsSpecifics().get(AmbariMetaInfo.ANY_OS);
          OsSpecific familyPackages = targetStack.getOsSpecificsSafe().get(osFamily);
          // !!! TODO get service specific package names

          Arrays.stream(new OsSpecific[] {anyPackages, familyPackages})
            .filter(osSpecific -> null != osSpecific && CollectionUtils.isNotEmpty(osSpecific.getPackages()))
            .forEach(osSpecific -> {
              packages.addAll(osSpecific.getPackages());
            });

          if (!details.containsKey(hostEntity)) {
            details.put(hostEntity, new HashSet<>());
          }

          MpackInstallDetail installDetail = new MpackInstallDetail(targetMpackEntity, packages);
          installDetail.serviceGroupName = serviceGroup.getServiceGroupName();
          details.get(hostEntity).add(installDetail);
        });
    }

    // !!! at this point we have a map of hosts and the installations that each one needs to run.

    RequestStageContainer stageContainer = createRequest();

    Iterator<Entry<HostEntity, Set<MpackInstallDetail>>> hostMapIterator = details.entrySet().iterator();

    int maxTasks = s_configuration.getAgentPackageParallelCommandsLimit();
    int hostCount = details.size();
    int batchCount = (int) (Math.ceil((double) hostCount / maxTasks));


    long stageId = stageContainer.getLastStageId() + 1;
    if (0L == stageId) {
      stageId = 1L;
    }

    ArrayList<Stage> stages = new ArrayList<>(batchCount);
    for (int batchId = 1; batchId <= batchCount; batchId++) {
      // Create next stage
      String stageName;
      if (batchCount > 1) {
        stageName = String.format(INSTALL_PACKAGES + ". Batch %d of %d", batchId,
            batchCount);
      } else {
        stageName = INSTALL_PACKAGES;
      }

      Stage stage = s_stageFactory.createNew(stageContainer.getId(), "/tmp/ambari", cluster.getClusterName(),
          cluster.getClusterId(), stageName, "{}", "{}");

      stage.getSuccessFactors().put(Role.valueOf(MPACK_PACKAGES_ACTION), successFactor);
      stage.setStageId(stageId);
      stageId++;

      // add the stage that was just created
      stages.add(stage);

      // Populate with commands for host
      for (int i = 0; i < maxTasks && hostMapIterator.hasNext(); i++) {
        Entry<HostEntity, Set<MpackInstallDetail>> entry = hostMapIterator.next();
        HostEntity host = entry.getKey();

        // add host to this stage
        RequestResourceFilter filter = new RequestResourceFilter(null, null, null,
                Collections.singletonList(host.getHostName()));

        List<OsSpecific.Package> packages = new ArrayList<>();
        entry.getValue().forEach(d -> { packages.addAll(d.mpackPackages); });

        Map<String, String> roleParams = ImmutableMap.<String, String>builder()
            .put(KeyNames.PACKAGE_LIST, s_gson.toJson(packages))
            .build();

        // !!! this loop PROBABLY won't work out, but leave for now to get things going
        for (MpackInstallDetail mpackDetail : entry.getValue()) {
          ActionExecutionContext actionContext = new ActionExecutionContext(cluster.getClusterName(),
              MPACK_PACKAGES_ACTION, Collections.singletonList(filter), roleParams);

          actionContext.setTimeout(Short.valueOf(s_configuration.getDefaultAgentTaskTimeout(true)));
          actionContext.setExpectedServiceGroupName(mpackDetail.serviceGroupName);

          Host h = cluster.getHost(host.getHostName());
          Mpack mpack = getManagementController().getAmbariMetaInfo().getMpack(mpackDetail.mpackId);
          RepoOsEntity repoOsEntity = s_repoHelper.getOSEntityForHost(mpackDetail.mpackEntity, h);

          // this isn't being placed correctly elsewhere
          actionContext.addVisitor(command -> {
            try {
              command.setClusterSettings(cluster.getClusterSettingsNameValueMap());
            } catch (AmbariException e) {
              LOG.warn("Could not set cluster settings on the command", e);
            }
          });

          s_repoHelper.addCommandRepositoryToContext(actionContext, mpack, repoOsEntity);

          s_actionExecutionHelper.get().addExecutionCommandsToStage(actionContext, stage, null);
        }
      }
    }

    stageContainer.addStages(stages);
    stageContainer.persist();

    return stageContainer;
  }

  @Override
  public RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
    throw new SystemException("Upgrade plan installs cannot be removed");
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return PK_PROPERTY_IDS;
  }

  private RequestStageContainer createRequest() {
    ActionManager actionManager = getManagementController().getActionManager();

    RequestStageContainer requestStages = new RequestStageContainer(
            actionManager.getNextRequestId(), null, s_requestFactory, actionManager);
    requestStages.setRequestContext(INSTALL_PACKAGES);

    return requestStages;
  }

  private static class MpackInstallDetail {
    private MpackEntity mpackEntity;
    private long mpackId = -1L;
    private List<OsSpecific.Package> mpackPackages = new ArrayList<>();
    private String serviceGroupName;

    private MpackInstallDetail(MpackEntity entity, List<OsSpecific.Package> packages) {
      mpackEntity = entity;
      mpackId = entity.getId();
      mpackPackages = packages;
    }
  }

}
