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
package org.apache.ambari.server.state;

import static org.apache.ambari.server.controller.internal.UpgradeResourceProvider.UPGRADE_FAIL_ON_CHECK_WARNINGS;
import static org.apache.ambari.server.controller.internal.UpgradeResourceProvider.UPGRADE_HOST_ORDERED_HOSTS;
import static org.apache.ambari.server.controller.internal.UpgradeResourceProvider.UPGRADE_PLAN_ID;
import static org.apache.ambari.server.controller.internal.UpgradeResourceProvider.UPGRADE_REVERT_UPGRADE_ID;
import static org.apache.ambari.server.controller.internal.UpgradeResourceProvider.UPGRADE_SKIP_FAILURES;
import static org.apache.ambari.server.controller.internal.UpgradeResourceProvider.UPGRADE_SKIP_MANUAL_VERIFICATION;
import static org.apache.ambari.server.controller.internal.UpgradeResourceProvider.UPGRADE_SKIP_PREREQUISITE_CHECKS;
import static org.apache.ambari.server.controller.internal.UpgradeResourceProvider.UPGRADE_SKIP_SC_FAILURES;
import static org.apache.ambari.server.controller.internal.UpgradeResourceProvider.UPGRADE_TYPE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.agent.ExecutionCommand.KeyNames;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.AbstractControllerResourceProvider;
import org.apache.ambari.server.controller.internal.PreUpgradeCheckResourceProvider;
import org.apache.ambari.server.controller.internal.UpgradeResourceProvider;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.dao.UpgradePlanDAO;
import org.apache.ambari.server.orm.entities.MpackEntity;
import org.apache.ambari.server.orm.entities.ServiceGroupEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.orm.entities.UpgradeHistoryEntity;
import org.apache.ambari.server.orm.entities.UpgradePlanDetailEntity;
import org.apache.ambari.server.orm.entities.UpgradePlanEntity;
import org.apache.ambari.server.stack.MasterHostResolver;
import org.apache.ambari.server.stageplanner.RoleGraphFactory;
import org.apache.ambari.server.state.Mpack.ModuleComponentVersionChange;
import org.apache.ambari.server.state.Mpack.ModuleVersionChange;
import org.apache.ambari.server.state.Mpack.MpackChangeSummary;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.Grouping;
import org.apache.ambari.server.state.stack.upgrade.HostOrderGrouping;
import org.apache.ambari.server.state.stack.upgrade.HostOrderItem;
import org.apache.ambari.server.state.stack.upgrade.HostOrderItem.HostOrderActionType;
import org.apache.ambari.server.state.stack.upgrade.LifecycleType;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * The {@link UpgradeContext} is used to hold all information pertaining to an
 * upgrade. It is initialized directly from an existing {@link UpgradeEntity} or
 * from a request to create an upgrade/downgrade.
 */
@Experimental(feature = ExperimentalFeature.UNIT_TEST_REQUIRED)
public class UpgradeContext {

  private static final Logger LOG = LoggerFactory.getLogger(UpgradeContext.class);

  public static final String COMMAND_PARAM_CLUSTER_NAME = "clusterName";
  public static final String COMMAND_PARAM_DIRECTION = "upgrade_direction";
  public static final String COMMAND_PARAM_UPGRADE_PACK = "upgrade_pack";
  public static final String COMMAND_PARAM_REQUEST_ID = "request_id";

  public static final String COMMAND_PARAM_UPGRADE_TYPE = "upgrade_type";
  public static final String COMMAND_PARAM_TASKS = "tasks";
  public static final String COMMAND_PARAM_STRUCT_OUT = "structured_out";

  /*
   * The cluster that the upgrade is for.
   */
  private final Cluster m_cluster;

  /**
   * The direction of the upgrade.
   */
  private final Direction m_direction;

  /**
   * The type of upgrade.
   */
  private final UpgradeType m_type;

  /**
   * The upgrade pack for this upgrade.
   */
  private UpgradePack m_upgradePack;

  /**
   * Resolves master components on hosts.
   */
  private final MasterHostResolver m_resolver;

  /**
   * A collection of hosts in the cluster which are unhealthy and will not
   * participate in the upgrade.
   */
  private final List<ServiceComponentHost> m_unhealthy = new ArrayList<>();

  /**
   * {@code true} if slave/client component failures should be automatically
   * skipped. This will only automatically skip the failure if the task is
   * skippable to begin with.
   */
  private boolean m_autoSkipComponentFailures = false;

  /**
   * {@code true} if service check failures should be automatically skipped.
   * This will only automatically skip the failure if the task is skippable to
   * begin with.
   */
  private boolean m_autoSkipServiceCheckFailures = false;

  /**
   * {@code true} if manual verification tasks should be automatically skipped.
   */
  private boolean m_autoSkipManualVerification = false;

  /**
   * A map containing each service group participating in the upgrade and the
   * services/components participating.
   */
  private final Map<ServiceGroup, MpackChangeSummary> m_serviceGroups = new HashMap<>();

  /**
   * Used by some {@link Grouping}s to generate commands. It is exposed here
   * mainly for injection purposes since the XML is not created by Guice.
   */
  @Inject
  private HostRoleCommandFactory m_hrcFactory;

  /**
   * Used by some {@link Grouping}s to determine command ordering. It is exposed
   * here mainly for injection purposes since the XML is not created by Guice.
   */
  @Inject
  private RoleGraphFactory m_roleGraphFactory;

  /**
   * Used for serializing the upgrade type.
   */
  @Inject
  private Gson m_gson;

  /**
   * Used for looking up information about components and services.
   */
  @Inject
  private AmbariMetaInfo m_metaInfo;

  /**
   * Used to suggest upgrade packs during creation of an upgrade context.
   */
  @Inject
  @Experimental(feature = ExperimentalFeature.MPACK_UPGRADES)
  private UpgradeHelper m_upgradeHelper;

  /**
   * Used to lookup a prior upgrade by ID.
   */
  @Inject
  private UpgradeDAO m_upgradeDAO;

  /**
   * Used as a quick way to tell if the upgrade is to revert a patch.
   */
  private final boolean m_isRevert;

  /**
   * The ID of the upgrade being reverted if this is a reversion.
   */
  private long m_revertUpgradeId;

  /**
   * Used to lookup overridable settings like default task parallelism
   */
  @Inject
  private Configuration configuration;

  /**
   * Constructor for {@link UpgradeContextFactory#create(Cluster, Map)} that is used
   * when making an upgrade context from {@link UpgradeResourceProvider}
   */
  @AssistedInject
  public UpgradeContext(@Assisted Cluster cluster,
      @Assisted Map<String, Object> upgradeRequestMap, Gson gson,
      AmbariMetaInfo metaInfo,
      UpgradeHelper upgradeHelper, UpgradeDAO upgradeDAO, ConfigHelper configHelper,
      UpgradePlanDAO upgradePlanDAO)
      throws AmbariException {
    // injected constructor dependencies
    m_gson = gson;
    m_upgradeDAO = upgradeDAO;
    m_cluster = cluster;
    m_metaInfo = metaInfo;
    m_isRevert = upgradeRequestMap.containsKey(UPGRADE_REVERT_UPGRADE_ID);


    if (m_isRevert) {
      m_revertUpgradeId = Long.valueOf(upgradeRequestMap.get(UPGRADE_REVERT_UPGRADE_ID).toString());
      UpgradeEntity revertUpgrade = m_upgradeDAO.findUpgrade(m_revertUpgradeId);
      UpgradeEntity revertableUpgrade = m_upgradeDAO.findRevertable(cluster.getClusterId());

      if (null == revertUpgrade) {
        throw new AmbariException(
            String.format("Could not find Upgrade with id %s to revert.", m_revertUpgradeId));
      }

      if (null == revertableUpgrade) {
        throw new AmbariException(
            String.format("There are no upgrades for cluster %s which are marked as revertable",
                cluster.getClusterName()));
      }

      if (revertUpgrade.getDirection() != Direction.UPGRADE) {
        throw new AmbariException(
            "Only successfully completed upgrades can be reverted. Downgrades cannot be reverted.");
      }

      if (!revertableUpgrade.getId().equals(revertUpgrade.getId())) {
        throw new AmbariException(String.format(
            "The only upgrade which is currently allowed to be reverted for cluster %s is upgrade ID %s which was an upgrade to the following mpacks: %s",
            cluster.getClusterName(), revertableUpgrade.getId(),
            revertableUpgrade.getTargetMpackStacks()));
      }

      m_type = calculateUpgradeType(upgradeRequestMap, revertUpgrade);

      // !!! build all service-specific reversions
      for (UpgradeHistoryEntity history : revertUpgrade.getHistory()) {
        ServiceGroupEntity serviceGroupEntity = history.getServiceGroupEntity();

        // reverse these on the revert
        MpackEntity sourceMpackEntity = history.getTargetMpackEntity();
        MpackEntity targetMpackEntity = history.getSourceMpackEntity();

        ServiceGroup serviceGroup = cluster.getServiceGroup(serviceGroupEntity.getServiceGroupId());

        // if the service group is no longer installed, do nothing
        if (null == serviceGroup) {
          LOG.warn(
              "The service group {} will not be reverted since it is no longer installed in the cluster",
              serviceGroupEntity.getServiceGroupName());

          continue;
        }

        Mpack sourceMpack = m_metaInfo.getMpack(sourceMpackEntity.getId());
        Mpack targetMpack = m_metaInfo.getMpack(targetMpackEntity.getId());
        MpackChangeSummary summary = sourceMpack.getChangeSummary(targetMpack);
        m_serviceGroups.put(serviceGroup, summary);
      }

      // !!! direction can ONLY be an downgrade on revert
      m_direction = Direction.DOWNGRADE;
    } else {
      if (!upgradeRequestMap.containsKey(UPGRADE_PLAN_ID)) {
        throw new AmbariException("An upgrade can only be started from an Upgrade Plan.");
      }

      Long upgradePlanId = Long.valueOf(upgradeRequestMap.get(UPGRADE_PLAN_ID).toString());
      UpgradePlanEntity upgradePlan = upgradePlanDAO.findByPK(upgradePlanId);

      m_direction = upgradePlan.getDirection();

      // depending on the direction, we must either have a target repository or an upgrade we are downgrading from
      switch(m_direction){
        case UPGRADE:{
          m_type = calculateUpgradeType(upgradeRequestMap, null);

          List<UpgradePlanDetailEntity> details = upgradePlan.getDetails();
          for (UpgradePlanDetailEntity detail : details) {
            long serviceGroupId = detail.getServiceGroupId();
            long targetMpackId = detail.getMpackTargetId();
            ServiceGroup serviceGroup = cluster.getServiceGroup(serviceGroupId);
            long sourceMpackId = serviceGroup.getMpackId();

            Mpack sourceMpack = m_metaInfo.getMpack(sourceMpackId);
            Mpack targetMpack = m_metaInfo.getMpack(targetMpackId);
            MpackChangeSummary summary = sourceMpack.getChangeSummary(targetMpack);
            if (!summary.hasVersionChanges()) {
              continue;
            }

            m_serviceGroups.put(serviceGroup, summary);
          }
          break;
        }
        case DOWNGRADE:{
          @Experimental(feature = ExperimentalFeature.MPACK_UPGRADES, comment = "Populate from prior upgrade")
          UpgradeEntity upgrade = m_upgradeDAO.findLastUpgradeForCluster(
              cluster.getClusterId(), Direction.UPGRADE);

          m_type = calculateUpgradeType(upgradeRequestMap, upgrade);
          populateParticipatingServiceGroups(cluster, m_serviceGroups, upgrade, true);
          break;
        }
        default:
          throw new AmbariException(
              String.format("%s is not a valid upgrade direction.", m_direction));
      }
    }

    // the validator will throw an exception if the upgrade request is not valid
    UpgradeRequestValidator upgradeRequestValidator = buildValidator(m_type);
    upgradeRequestValidator.validate(cluster, m_direction, m_type, m_upgradePack,
        upgradeRequestMap);

    // optionally skip failures - this can be supplied on either the request or
    // in the upgrade pack explicitely, however the request will always override
    // the upgrade pack if explicitely specified
    boolean skipComponentFailures = false;
    boolean skipServiceCheckFailures = false;

    // only override the upgrade pack if set on the request
    if (upgradeRequestMap.containsKey(UPGRADE_SKIP_FAILURES)) {
      skipComponentFailures = Boolean.parseBoolean(
          (String) upgradeRequestMap.get(UPGRADE_SKIP_FAILURES));
    }

    // only override the upgrade pack if set on the request
    if (upgradeRequestMap.containsKey(UPGRADE_SKIP_SC_FAILURES)) {
      skipServiceCheckFailures = Boolean.parseBoolean(
          (String) upgradeRequestMap.get(UPGRADE_SKIP_SC_FAILURES));
    }

    boolean skipManualVerification = false;
    if (upgradeRequestMap.containsKey(UPGRADE_SKIP_MANUAL_VERIFICATION)) {
      skipManualVerification = Boolean.parseBoolean(
          (String) upgradeRequestMap.get(UPGRADE_SKIP_MANUAL_VERIFICATION));
    }

    m_autoSkipComponentFailures = skipComponentFailures;
    m_autoSkipServiceCheckFailures = skipServiceCheckFailures;
    m_autoSkipManualVerification = skipManualVerification;

    m_resolver = new MasterHostResolver(m_cluster, configHelper, this);
  }

  /**
   * Constructor for {@link UpgradeContextFactory#create(Cluster, UpgradeEntity)} that
   * loads an upgrade context from storage.
   *
   * @param cluster
   *          the cluster that the upgrade is for
   * @param upgradeEntity
   *          the upgrade entity
   */
  @AssistedInject
  public UpgradeContext(@Assisted Cluster cluster, @Assisted UpgradeEntity upgradeEntity,
      AmbariMetaInfo ambariMetaInfo, ConfigHelper configHelper) throws AmbariException {
    m_metaInfo = ambariMetaInfo;

    m_cluster = cluster;
    m_type = upgradeEntity.getUpgradeType();
    m_direction = upgradeEntity.getDirection();

    m_autoSkipComponentFailures = upgradeEntity.isComponentFailureAutoSkipped();
    m_autoSkipServiceCheckFailures = upgradeEntity.isServiceCheckFailureAutoSkipped();

    populateParticipatingServiceGroups(cluster, m_serviceGroups, upgradeEntity, false);

    m_resolver = new MasterHostResolver(m_cluster, configHelper, this);
    m_isRevert = upgradeEntity.isRevert();
  }

  /**
   * Gets the upgrade pack for this upgrade.
   *
   * @return the upgrade pack
   */
  public UpgradePack getUpgradePack() {
    return m_upgradePack;
  }

  /**
   * Sets the upgrade pack for this upgrade
   *
   * @param upgradePack
   *          the upgrade pack to set
   */
  public void setUpgradePack(UpgradePack upgradePack) {
    m_upgradePack = upgradePack;
  }

  /**
   * Gets the cluster that the upgrade is for.
   *
   * @return the cluster (never {@code null}).
   */
  public Cluster getCluster() {
    return m_cluster;
  }

  /**
   * @return the direction of the upgrade
   */
  public Direction getDirection() {
    return m_direction;
  }

  /**
   * @return the type of upgrade.
   */
  public UpgradeType getType() {
    return m_type;
  }

  /**
   * @return the resolver
   */
  public MasterHostResolver getResolver() {
    return m_resolver;
  }

  /**
   * @return the metainfo for access to service definitions
   */
  public AmbariMetaInfo getAmbariMetaInfo() {
    return m_metaInfo;
  }

  /**
   * @param unhealthy a list of unhealthy host components
   */
  public void addUnhealthy(List<ServiceComponentHost> unhealthy) {
    m_unhealthy.addAll(unhealthy);
  }

  /**
   * Gets whether skippable components that failed are automatically skipped.
   *
   * @return the skipComponentFailures
   */
  public boolean isComponentFailureAutoSkipped() {
    return m_autoSkipComponentFailures;
  }

  /**
   * Gets whether skippable service checks that failed are automatically
   * skipped.
   *
   * @return the skipServiceCheckFailures
   */
  public boolean isServiceCheckFailureAutoSkipped() {
    return m_autoSkipServiceCheckFailures;
  }

  /**
   * Gets whether manual verification tasks can be automatically skipped.
   *
   * @return the skipManualVerification
   */
  public boolean isManualVerificationAutoSkipped() {
    return m_autoSkipManualVerification;
  }

  /**
   * Gets the injected instance of a {@link RoleGraphFactory}.
   *
   * @return a {@link RoleGraphFactory} instance (never {@code null}).
   */
  public RoleGraphFactory getRoleGraphFactory() {
    return m_roleGraphFactory;
  }

  /**
   * Gets the injected instance of a {@link HostRoleCommandFactory}.
   *
   * @return a {@link HostRoleCommandFactory} instance (never {@code null}).
   */
  public HostRoleCommandFactory getHostRoleCommandFactory() {
    return m_hrcFactory;
  }

  /**
   * Gets a map initialized with parameters required for upgrades to work. The
   * following properties are already set:
   * <ul>
   * <li>{@link #COMMAND_PARAM_CLUSTER_NAME}
   * <li>{@link #COMMAND_PARAM_DIRECTION}
   * <li>{@link #COMMAND_PARAM_UPGRADE_TYPE}
   * <li>{@link KeyNames#REFRESH_CONFIG_TAGS_BEFORE_EXECUTION} - necessary in
   * order to have the commands contain the correct configurations. Otherwise,
   * they will contain the configurations that were available at the time the
   * command was created. For upgrades, this is problematic since the commands
   * are all created ahead of time, but the upgrade may change configs as part
   * of the upgrade pack.</li>
   * <ul>
   *
   * @return the initialized parameter map.
   */
  public Map<String, String> getInitializedCommandParameters() {
    Map<String, String> parameters = new HashMap<>();

    Direction direction = getDirection();
    parameters.put(COMMAND_PARAM_CLUSTER_NAME, m_cluster.getClusterName());
    parameters.put(COMMAND_PARAM_DIRECTION, direction.name().toLowerCase());

    if (null != getType()) {
      // use the serialized attributes of the enum to convert it to a string,
      // but first we must convert it into an element so that we don't get a
      // quoted string - using toString() actually returns a quoted stirng which
      // is bad
      JsonElement json = m_gson.toJsonTree(getType());
      parameters.put(COMMAND_PARAM_UPGRADE_TYPE, json.getAsString());
    }

    parameters.put(KeyNames.REFRESH_CONFIG_TAGS_BEFORE_EXECUTION, "true");
    return parameters;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("direction", m_direction)
        .add("type", m_type).toString();
  }

  /**
   * Gets whether a downgrade is allowed for this upgrade. If the direction is
   * {@link Direction#DOWNGRADE}, then this method always returns false.
   * Otherwise it will consule {@link UpgradePack#isDowngradeAllowed()}.
   *
   * @return {@code true} of a downgrade is allowed for this upgrade,
   *         {@code false} otherwise.
   */
  public boolean isDowngradeAllowed() {
    if (m_direction == Direction.DOWNGRADE) {
      return false;
    }

    return m_upgradePack.isDowngradeAllowed();
  }

  /**
   * @return
   */
  public boolean isPatchRevert() {
    return m_isRevert;
  }

  public long getPatchRevertUpgradeId() {
    return m_revertUpgradeId;
  }

  /**
   * Gets whether this upgrade is revertable.
   *
   * @return
   */
  @Experimental(
      feature = ExperimentalFeature.MPACK_UPGRADES,
      comment = "This must be stored in an upgrade pack or calculated from the versions")
  public boolean isRevertable() {
    throw new NotImplementedException();
  }

  /**
   * @return default value of number of tasks to run in parallel during upgrades
   */
  public int getDefaultMaxDegreeOfParallelism() {
    return configuration.getDefaultMaxParallelismForUpgrades();
  }

  /**
   * Gets a POJO of the upgrade suitable to serialize.
   *
   * @return the upgrade summary as a POJO.
   */
  public UpgradeSummary getUpgradeSummary() {
    UpgradeSummary summary = new UpgradeSummary();
    summary.direction = m_direction;
    summary.isRevert = m_isRevert;
    summary.serviceGroups = new HashMap<>();

    for (ServiceGroup serviceGroup : m_serviceGroups.keySet()) {
      MpackChangeSummary changeSummary = m_serviceGroups.get(serviceGroup);

      UpgradeServiceGroupSummary serviceGroupSummary = new UpgradeServiceGroupSummary();
      serviceGroupSummary.type = m_type;
      serviceGroupSummary.sourceMpackId = changeSummary.getSource().getResourceId();
      serviceGroupSummary.sourceStack = changeSummary.getSource().getStackId().getStackId();
      serviceGroupSummary.targetMpackId = changeSummary.getTarget().getRegistryId();
      serviceGroupSummary.targetStack = changeSummary.getTarget().getStackId().getStackId();
      serviceGroupSummary.services = new LinkedHashMap<>();

      summary.serviceGroups.put(serviceGroup.getServiceGroupName(), serviceGroupSummary);

      for( ModuleVersionChange moduleVersionChange : changeSummary.getModuleVersionChanges() ) {
        UpgradeServiceSummary upgradeServiceSummary = new UpgradeServiceSummary();
        upgradeServiceSummary.sourceVersion = moduleVersionChange.getSource().getVersion();
        upgradeServiceSummary.targetVersion = moduleVersionChange.getTarget().getVersion();
        upgradeServiceSummary.components = new LinkedHashMap<>();
        serviceGroupSummary.services.put(moduleVersionChange.getSource().getName(), upgradeServiceSummary);

        for( ModuleComponentVersionChange componentVersionChange : moduleVersionChange.getComponentChanges() ) {
          UpgradeComponentSummary componentSummary = new UpgradeComponentSummary();
          componentSummary.sourceVersion = componentVersionChange.getSource().getVersion();
          componentSummary.targetVersion = componentVersionChange.getTarget().getVersion();

          upgradeServiceSummary.components.put(componentVersionChange.getSource().getName(), componentSummary);
        }
      }
    }

    return summary;
  }

  /**
   * Gets the service groups participating in the upgrade, mapped to their
   * respective {@link MpackChangeSummary}s.
   *
   * @return the service groups in the upgrade.
   */
  public Map<ServiceGroup, MpackChangeSummary> getServiceGroups() {
    return m_serviceGroups;
  }

  /**
   * Gets the target management packs for this upgrade.
   *
   * @return the target mpacks for all service groups participating.
   */
  public Set<Mpack> getTargetMpacks() {
    return m_serviceGroups.values().stream().map(serviceGroup -> serviceGroup.getTarget()).collect(
        Collectors.toSet());
  }

  /**
   * Gets the source mpack for the specified service group in this upgrade.
   *
   * @param serviceGroup
   *          the service group
   * @return the source mpack, or {@code null} if the service group is not
   *         participating.
   */
  public Mpack getSourceMpack(ServiceGroup serviceGroup) {
    Mpack source = null;
    MpackChangeSummary changeSummary = m_serviceGroups.get(serviceGroup);
    if (null != changeSummary) {
      source = changeSummary.getSource();
    }

    return source;
  }

  /**
   * Gets the target mpack for the specified service group in this upgrade.
   *
   * @param serviceGroup
   *          the service group
   * @return the target mpack, or {@code null} if the service group is not
   *         participating.
   */
  public Mpack getTargetMpack(ServiceGroup serviceGroup) {
    Mpack target = null;
    MpackChangeSummary changeSummary = m_serviceGroups.get(serviceGroup);
    if (null != changeSummary) {
      target = changeSummary.getTarget();
    }

    return target;
  }

  /**
   * Gets whether the service is supported in this upgrade.
   *
   * @param serviceName
   * @return
   */
  @Experimental(feature=ExperimentalFeature.MPACK_UPGRADES, comment = "Needs implementation and thought")
  public boolean isSupportedInUpgrade(String serviceName) {
    return false;
  }

  /**
   * Gets the display name for a given service.
   *
   * @param mpack
   *          the mpack which owns the service.
   * @param serviceName
   *          the service name.
   * @return the service display name.
   */
  public String getDisplayName(Mpack mpack, String serviceName) {
    Module module = mpack.getModule(serviceName);
    if (null == module) {
      return serviceName;
    }

    return module.getDisplayName();

  }

  /**
   * Gets the display name for a given component.
   *
   * @param mpack
   *          the mpack which owns the service.
   * @param serviceName
   *          the component's service.
   * @param componentName
   *          the component name.
   * @return the component display name.
   */
  public String getDisplayName(Mpack mpack, String serviceName, String componentName) {
    ModuleComponent moduleComponent = mpack.getModuleComponent(serviceName, componentName);
    if (null == moduleComponent) {
      return componentName;
    }

    return moduleComponent.getName();
  }

  /**
   * Gets a displayable summary of the service groups and their upgrade
   * information.
   *
   * @return a displayable summary of the upgrade at the service group level.
   */
  public String getServiceGroupDisplayableSummary() {
    StringBuilder buffer = new StringBuilder();
    for (ServiceGroup serviceGroup : m_serviceGroups.keySet()) {
      MpackChangeSummary changeSummary = m_serviceGroups.get(serviceGroup);
      Mpack source = changeSummary.getSource();
      Mpack target = changeSummary.getTarget();

      buffer.append(serviceGroup.getServiceGroupName()).append(": ").append(
          source.getStackId()).append("->").append(target.getStackId()).append(
              System.lineSeparator());
    }

    return buffer.toString();
  }

  /**
   * Reading upgrade type from provided request or if nothing were provided,
   * from previous upgrade for downgrade direction.
   *
   * @param upgradeRequestMap
   *          arguments provided for current upgrade request
   * @param upgradeEntity
   *          previous upgrade entity, should be passed only for downgrade
   *          direction
   *
   * @return
   * @throws AmbariException
   */
  private UpgradeType calculateUpgradeType(Map<String, Object> upgradeRequestMap,
                                           UpgradeEntity upgradeEntity) throws AmbariException {

    UpgradeType upgradeType = UpgradeType.ROLLING;

    String upgradeTypeProperty = (String) upgradeRequestMap.get(UPGRADE_TYPE);
    boolean upgradeTypePassed = StringUtils.isNotBlank(upgradeTypeProperty);

    if (upgradeTypePassed){
      try {
        upgradeType = UpgradeType.valueOf(upgradeRequestMap.get(UPGRADE_TYPE).toString());
      } catch (Exception e) {
        throw new AmbariException(String.format("Property %s has an incorrect value of %s.",
          UPGRADE_TYPE, upgradeTypeProperty));
      }
    } else if (upgradeEntity != null){
      upgradeType = upgradeEntity.getUpgradeType();
    }

    return upgradeType;
  }

  /**
   * Populate the participating service groups and their respective mpack
   * summary differences. This is only for an upgrade which has already been
   * created and persisted.
   *
   * @param cluster
   * @param serviceGroups
   * @param upgrade
   * @param reverse
   * @throws AmbariException
   */
  private void populateParticipatingServiceGroups(Cluster cluster,
      Map<ServiceGroup, MpackChangeSummary> serviceGroups, UpgradeEntity upgrade,
      boolean reverse) throws AmbariException {

    for (UpgradeHistoryEntity history : upgrade.getHistory()) {
      ServiceGroupEntity serviceGroupEntity = history.getServiceGroupEntity();
      ServiceGroup serviceGroup = cluster.getServiceGroup(serviceGroupEntity.getServiceGroupId());

      // reverse these on the downgrade
      MpackEntity sourceMpackEntity = reverse ? history.getTargetMpackEntity() : history.getSourceMpackEntity();
      MpackEntity targetMpackEntity = reverse ? history.getSourceMpackEntity() : history.getTargetMpackEntity();

      Mpack sourceMpack = m_metaInfo.getMpack(sourceMpackEntity.getId());
      Mpack targetMpack = m_metaInfo.getMpack(targetMpackEntity.getId());
      MpackChangeSummary summary = sourceMpack.getChangeSummary(targetMpack);
      serviceGroups.put(serviceGroup, summary);
    }
  }

  /**
   * Builds a chain of {@link UpgradeRequestValidator}s to ensure that the
   * incoming request to create a new upgrade is valid.
   *
   * @param upgradeType
   *          the type of upgrade to build the validator for.
   * @return the validator which can check to ensure that the properties are
   *         valid.
   */
  private UpgradeRequestValidator buildValidator(UpgradeType upgradeType){
    UpgradeRequestValidator validator = new BasicUpgradePropertiesValidator();
    UpgradeRequestValidator preReqValidator = new PreReqCheckValidator();
    validator.setNextValidator(preReqValidator);

    final UpgradeRequestValidator upgradeTypeValidator;
    switch (upgradeType) {
      case HOST_ORDERED:
        upgradeTypeValidator = new HostOrderedUpgradeValidator();
        break;
      case EXPRESS:
      case ROLLING:
      default:
        upgradeTypeValidator = null;
        break;
    }

    preReqValidator.setNextValidator(upgradeTypeValidator);
    return validator;
  }

  /**
   * The {@link UpgradeRequestValidator} contains the logic to check for correct
   * upgrade request properties and then pass the responsibility onto the next
   * validator in the chain.
   */
  private abstract class UpgradeRequestValidator {
    /**
     * The next validator.
     */
    UpgradeRequestValidator m_nextValidator;

    /**
     * Sets the next validator in the chain.
     *
     * @param nextValidator
     *          the next validator to run, or {@code null} for none.
     */
    void setNextValidator(UpgradeRequestValidator nextValidator) {
      m_nextValidator = nextValidator;
    }

    /**
     * Validates the upgrade request from this point in the chain.
     *
     * @param cluster
     * @param direction
     * @param type
     * @param upgradePack
     * @param requestMap
     * @throws AmbariException
     */
    final void validate(Cluster cluster, Direction direction, UpgradeType type,
        UpgradePack upgradePack, Map<String, Object> requestMap) throws AmbariException {

      // run this instance's check
      check(cluster, direction, type, upgradePack, requestMap);

      // pass along to the next
      if (null != m_nextValidator) {
        m_nextValidator.validate(cluster, direction, type, upgradePack, requestMap);
      }
    }

    /**
     * Checks to ensure that upgrade request is valid given the specific
     * arguments.
     *
     * @param cluster
     * @param direction
     * @param type
     * @param upgradePack
     * @param requestMap
     * @throws AmbariException
     */
    abstract void check(Cluster cluster, Direction direction, UpgradeType type,
        UpgradePack upgradePack, Map<String, Object> requestMap) throws AmbariException;
  }

  /**
   * The {@link BasicUpgradePropertiesValidator} ensures that the basic required
   * properties are present on the upgrade request.
   */
  private final class BasicUpgradePropertiesValidator extends UpgradeRequestValidator {

    /**
     * {@inheritDoc}
     */
    @Override
    public void check(Cluster cluster, Direction direction, UpgradeType type,
        UpgradePack upgradePack, Map<String, Object> requestMap) throws AmbariException {

      if (direction == Direction.UPGRADE) {
        String upgradePlanId = (String) requestMap.get(UPGRADE_PLAN_ID);
        if (StringUtils.isBlank(upgradePlanId)) {
          throw new AmbariException(
              String.format("%s is required for upgrades", UPGRADE_PLAN_ID));
        }
      }
    }
  }

  /**
   * The {@link PreReqCheckValidator} ensures that the upgrade pre-requisite
   * checks have passed.
   */
  private final class PreReqCheckValidator extends UpgradeRequestValidator {
    /**
     * {@inheritDoc}
     */
    @Override
    void check(Cluster cluster, Direction direction, UpgradeType type, UpgradePack upgradePack,
        Map<String, Object> requestMap) throws AmbariException {

      String upgradePlanId = (String) requestMap.get(UPGRADE_PLAN_ID);
      boolean skipPrereqChecks = Boolean.parseBoolean((String) requestMap.get(UPGRADE_SKIP_PREREQUISITE_CHECKS));
      boolean failOnCheckWarnings = Boolean.parseBoolean((String) requestMap.get(UPGRADE_FAIL_ON_CHECK_WARNINGS));

      // verify that there is not an upgrade or downgrade that is in progress or suspended
      UpgradeEntity existingUpgrade = cluster.getUpgradeInProgress();
      if (null != existingUpgrade) {
        throw new AmbariException(
            String.format("Unable to perform %s as another %s (request ID %s) is in progress.",
                direction.getText(false), existingUpgrade.getDirection().getText(false),
                existingUpgrade.getRequestId()));
      }

      // skip this check if it's a downgrade or we are instructed to skip it
      if (direction.isDowngrade() || skipPrereqChecks) {
        return;
      }

      // Validate pre-req checks pass
      PreUpgradeCheckResourceProvider provider = (PreUpgradeCheckResourceProvider) AbstractControllerResourceProvider.getResourceProvider(
          Resource.Type.PreUpgradeCheck);

      Predicate preUpgradeCheckPredicate = new PredicateBuilder().property(
          PreUpgradeCheckResourceProvider.UPGRADE_CHECK_CLUSTER_NAME_PROPERTY_ID).equals(cluster.getClusterName()).and().property(
          PreUpgradeCheckResourceProvider.UPGRADE_CHECK_UPGRADE_PLAN_ID).equals(upgradePlanId).and().property(
          PreUpgradeCheckResourceProvider.UPGRADE_CHECK_UPGRADE_TYPE_PROPERTY_ID).equals(type).toPredicate();

      Request preUpgradeCheckRequest = PropertyHelper.getReadRequest();

      Set<Resource> preUpgradeCheckResources;
      try {
        preUpgradeCheckResources = provider.getResources(
            preUpgradeCheckRequest, preUpgradeCheckPredicate);
      } catch (NoSuchResourceException|SystemException|UnsupportedPropertyException|NoSuchParentResourceException e) {
        throw new AmbariException(
            String.format("Unable to perform %s. Prerequisite checks could not be run",
                direction.getText(false)), e);
      }

      List<Resource> failedResources = new LinkedList<>();
      if (preUpgradeCheckResources != null) {
        for (Resource res : preUpgradeCheckResources) {
          PrereqCheckStatus prereqCheckStatus = (PrereqCheckStatus) res.getPropertyValue(
              PreUpgradeCheckResourceProvider.UPGRADE_CHECK_STATUS_PROPERTY_ID);

          if (prereqCheckStatus == PrereqCheckStatus.FAIL
              || (failOnCheckWarnings && prereqCheckStatus == PrereqCheckStatus.WARNING)) {
            failedResources.add(res);
          }
        }
      }

      if (!failedResources.isEmpty()) {
        throw new AmbariException(
            String.format("Unable to perform %s. Prerequisite checks failed %s",
                direction.getText(false), m_gson.toJson(failedResources)));
      }
    }
  }

  /**
   * Ensures that for {@link UpgradeType#HOST_ORDERED}, the properties supplied
   * are valid.
   */
  @SuppressWarnings("unchecked")
  private final class HostOrderedUpgradeValidator extends UpgradeRequestValidator {

    /**
     * {@inheritDoc}
     */
    @Override
    void check(Cluster cluster, Direction direction, UpgradeType type, UpgradePack upgradePack,
        Map<String, Object> requestMap) throws AmbariException {

      String skipFailuresRequestProperty = (String) requestMap.get(UPGRADE_SKIP_FAILURES);
      if (Boolean.parseBoolean(skipFailuresRequestProperty)) {
        throw new AmbariException(
            String.format("The %s property is not valid when creating a %s upgrade.",
                UPGRADE_SKIP_FAILURES, UpgradeType.HOST_ORDERED));
      }

      String skipManualVerification = (String) requestMap.get(UPGRADE_SKIP_MANUAL_VERIFICATION);
      if (Boolean.parseBoolean(skipManualVerification)) {
        throw new AmbariException(
            String.format("The %s property is not valid when creating a %s upgrade.",
                UPGRADE_SKIP_MANUAL_VERIFICATION, UpgradeType.HOST_ORDERED));
      }

      if (!requestMap.containsKey(UPGRADE_HOST_ORDERED_HOSTS)) {
        throw new AmbariException(
            String.format("The %s property is required when creating a %s upgrade.",
                UPGRADE_HOST_ORDERED_HOSTS, UpgradeType.HOST_ORDERED));
      }

      List<HostOrderItem> hostOrderItems = extractHostOrderItemsFromRequest(requestMap);
      List<String> hostsFromRequest = new ArrayList<>(hostOrderItems.size());
      for (HostOrderItem hostOrderItem : hostOrderItems) {
        if (hostOrderItem.getType() == HostOrderActionType.HOST_UPGRADE) {
          hostsFromRequest.addAll(hostOrderItem.getActionItems());
        }
      }

      // ensure that all hosts for this cluster are accounted for
      Collection<Host> hosts = cluster.getHosts();
      Set<String> clusterHostNames = new HashSet<>(hosts.size());
      for (Host host : hosts) {
        clusterHostNames.add(host.getHostName());
      }

      Collection<String> disjunction = CollectionUtils.disjunction(hostsFromRequest,
          clusterHostNames);

      if (CollectionUtils.isNotEmpty(disjunction)) {
        throw new AmbariException(String.format(
            "The supplied list of hosts must match the cluster hosts in an upgrade of type %s. The following hosts are either missing or invalid: %s",
            UpgradeType.HOST_ORDERED, StringUtils.join(disjunction, ", ")));
      }

      // verify that the upgradepack has the required grouping and set the
      // action items on it
      HostOrderGrouping hostOrderGrouping = null;
      List<Grouping> groupings = upgradePack.getGroups(LifecycleType.UPGRADE, direction);
      for (Grouping grouping : groupings) {
        if (grouping instanceof HostOrderGrouping) {
          hostOrderGrouping = (HostOrderGrouping) grouping;
          hostOrderGrouping.setHostOrderItems(hostOrderItems);
        }
      }
    }

    /**
     * Builds the list of {@link HostOrderItem}s from the upgrade request. If
     * the upgrade request does not contain the hosts
     *
     * @param requestMap
     *          the map of properties from the request (not {@code null}).
     * @return the ordered list of actions to orchestrate for the
     *         {@link UpgradeType#HOST_ORDERED} upgrade.
     * @throws AmbariException
     *           if the request properties are not valid.
     */
    private List<HostOrderItem> extractHostOrderItemsFromRequest(Map<String, Object> requestMap)
        throws AmbariException {
      // ewwww
      Set<Map<String, List<String>>> hostsOrder = (Set<Map<String, List<String>>>) requestMap.get(
          UPGRADE_HOST_ORDERED_HOSTS);

      if (CollectionUtils.isEmpty(hostsOrder)) {
        throw new AmbariException(
            String.format("The %s property must be specified when using a %s upgrade type.",
                UPGRADE_HOST_ORDERED_HOSTS, UpgradeType.HOST_ORDERED));
      }

      List<HostOrderItem> hostOrderItems = new ArrayList<>();

      // extract all of the hosts so that we can ensure they are all accounted
      // for
      Iterator<Map<String, List<String>>> iterator = hostsOrder.iterator();
      while (iterator.hasNext()) {
        Map<String, List<String>> grouping = iterator.next();
        List<String> hosts = grouping.get("hosts");
        List<String> serviceChecks = grouping.get("service_checks");

        if (CollectionUtils.isEmpty(hosts) && CollectionUtils.isEmpty(serviceChecks)) {
          throw new AmbariException(String.format(
              "The %s property must contain at least one object with either a %s or %s key",
              UPGRADE_HOST_ORDERED_HOSTS, "hosts", "service_checks"));
        }

        if (CollectionUtils.isNotEmpty(hosts)) {
          hostOrderItems.add(new HostOrderItem(HostOrderActionType.HOST_UPGRADE, hosts));
        }

        if (CollectionUtils.isNotEmpty(serviceChecks)) {
          hostOrderItems.add(new HostOrderItem(HostOrderActionType.SERVICE_CHECK, serviceChecks));
        }
      }

      return hostOrderItems;
    }
  }

  /**
   * The {@link UpgradeSummary} class is a simple POJO used to serialize the
   * infomration about and upgrade.
   */
  public static class UpgradeSummary {
    @SerializedName("direction")
    public Direction direction;

    @SerializedName("isRevert")
    public boolean isRevert = false;

    /**
     * Mapping of service group name to the service group in the upgrade.
     */
    @SerializedName("serviceGroups")
    public Map<String, UpgradeServiceGroupSummary> serviceGroups;
  }

  /**
   * The {@link UpgradeServiceGroupSummary} class is used as a way to
   * encapsulate the service group components and upgrade type participating in
   * the upgrade.
   */
  public static class UpgradeServiceGroupSummary {
    @SerializedName("type")
    public UpgradeType type;

    @SerializedName("sourceMpackId")
    public long sourceMpackId;

    @SerializedName("targetMpackId")
    public long targetMpackId;

    @SerializedName("sourceStack")
    public String sourceStack;

    @SerializedName("targetStack")
    public String targetStack;

    /**
     * A mapping of service name to service summary information for services
     * participating in the upgrade for this service group.
     */
    @SerializedName("services")
    public Map<String, UpgradeServiceSummary> services;
  }

  /**
   * The {@link UpgradeServiceSummary} class is used as a way to encapsulate the
   * service component upgrade information during an upgrade.
   */
  public static class UpgradeServiceSummary {
    @SerializedName("sourceVersion")
    public String sourceVersion;

    @SerializedName("targetVersion")
    public String targetVersion;

    /**
     * Mapping of component name its com
     */
    @SerializedName("components")
    public Map<String, UpgradeComponentSummary> components;
  }

  /**
   * The {@link UpgradeComponentSummary} class is used as a way to encapsulate
   * the component source and target versions during an upgrade.
   */
  public static class UpgradeComponentSummary {
    @SerializedName("sourceVersion")
    public String sourceVersion;

    @SerializedName("targetVersion")
    public String targetVersion;
  }
}