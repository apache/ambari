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

import static org.apache.ambari.server.controller.internal.UpgradeResourceProvider.UPGRADE_DIRECTION;
import static org.apache.ambari.server.controller.internal.UpgradeResourceProvider.UPGRADE_FAIL_ON_CHECK_WARNINGS;
import static org.apache.ambari.server.controller.internal.UpgradeResourceProvider.UPGRADE_HOST_ORDERED_HOSTS;
import static org.apache.ambari.server.controller.internal.UpgradeResourceProvider.UPGRADE_PACK;
import static org.apache.ambari.server.controller.internal.UpgradeResourceProvider.UPGRADE_REPO_VERSION_ID;
import static org.apache.ambari.server.controller.internal.UpgradeResourceProvider.UPGRADE_REVERT_UPGRADE_ID;
import static org.apache.ambari.server.controller.internal.UpgradeResourceProvider.UPGRADE_SKIP_FAILURES;
import static org.apache.ambari.server.controller.internal.UpgradeResourceProvider.UPGRADE_SKIP_MANUAL_VERIFICATION;
import static org.apache.ambari.server.controller.internal.UpgradeResourceProvider.UPGRADE_SKIP_PREREQUISITE_CHECKS;
import static org.apache.ambari.server.controller.internal.UpgradeResourceProvider.UPGRADE_SKIP_SC_FAILURES;
import static org.apache.ambari.server.controller.internal.UpgradeResourceProvider.UPGRADE_TYPE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.agent.ExecutionCommand.KeyNames;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.internal.AbstractControllerResourceProvider;
import org.apache.ambari.server.controller.internal.PreUpgradeCheckResourceProvider;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.orm.entities.UpgradeHistoryEntity;
import org.apache.ambari.server.stack.MasterHostResolver;
import org.apache.ambari.server.stageplanner.RoleGraphFactory;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.Grouping;
import org.apache.ambari.server.state.stack.upgrade.HostOrderGrouping;
import org.apache.ambari.server.state.stack.upgrade.HostOrderItem;
import org.apache.ambari.server.state.stack.upgrade.HostOrderItem.HostOrderActionType;
import org.apache.ambari.server.state.stack.upgrade.UpgradeScope;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * The {@link UpgradeContext} is used to hold all information pertaining to an
 * upgrade. It is initialized directly from an existing {@link UpgradeEntity} or
 * from a request to create an upgrade/downgrade.
 */
public class UpgradeContext {

  private static final Logger LOG = LoggerFactory.getLogger(UpgradeContext.class);

  public static final String COMMAND_PARAM_CLUSTER_NAME = "clusterName";
  public static final String COMMAND_PARAM_DIRECTION = "upgrade_direction";
  public static final String COMMAND_PARAM_UPGRADE_PACK = "upgrade_pack";
  public static final String COMMAND_PARAM_REQUEST_ID = "request_id";

  public static final String COMMAND_PARAM_UPGRADE_TYPE = "upgrade_type";
  public static final String COMMAND_PARAM_TASKS = "tasks";
  public static final String COMMAND_PARAM_STRUCT_OUT = "structured_out";

  @Deprecated
  @Experimental(
      feature = ExperimentalFeature.PATCH_UPGRADES,
      comment = "This isn't needed anymore, but many python classes still use it")
  public static final String COMMAND_PARAM_DOWNGRADE_FROM_VERSION = "downgrade_from_version";

  /*
   * The cluster that the upgrade is for.
   */
  final private Cluster m_cluster;

  /**
   * The direction of the upgrade.
   */
  final private Direction m_direction;

  /**
   * The type of upgrade.
   */
  final private UpgradeType m_type;

  /**
   * The upgrade pack for this upgrade.
   */
  private UpgradePack m_upgradePack;

  /**
   * Upgrades will always have a single version being upgraded to and downgrades
   * will have a single version being downgraded from. This repository
   * represents that version.
   * <p/>
   * When the direction is {@link Direction#UPGRADE}, this represents the target
   * repository. <br/>
   * When the direction is {@link Direction#DOWNGRADE}, this represents the
   * repository being downgraded from.
   */
  private final RepositoryVersionEntity m_repositoryVersion;

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
   * Mapping of service name to display name.
   */
  private final Map<String, String> m_serviceNames = new HashMap<>();

  /**
   * Mapping of component name to display name.
   */
  private final Map<String, String> m_componentNames = new HashMap<>();

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
   * A set of services which are included in this upgrade. If this is empty,
   * then all cluster services are included.
   */
  private Set<String> m_services = new HashSet<>();

  /**
   * A mapping of service to target repository. On an upgrade, this will be the
   * same for all services. On a downgrade, this may be different for each
   * service depending on which repository the service was on before the failed
   * upgrade.
   */
  private final Map<String, RepositoryVersionEntity> m_targetRepositoryMap = new HashMap<>();

  /**
   * A mapping of service to source (from) repository. On an upgrade, this will
   * be the current desired repository of every service. When downgrading, this
   * will be the same for all components and will represent the value returned
   * from {@link #getRepositoryVersion()}.
   */
  private final Map<String, RepositoryVersionEntity> m_sourceRepositoryMap = new HashMap<>();

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
  private UpgradeHelper m_upgradeHelper;

  /**
   * Used to lookup the repository version from an ID.
   */
  @Inject
  private RepositoryVersionDAO m_repoVersionDAO;

  /**
   * Used to lookup a prior upgrade by ID.
   */
  @Inject
  private UpgradeDAO m_upgradeDAO;

  /**
   * Used as a quick way to tell if the upgrade is to revert a patch.
   */
  private boolean m_isRevert = false;

  /**
   * Defines orchestration type.  This is not the repository type when reverting a patch.
   */
  private RepositoryType m_orchestration = RepositoryType.STANDARD;

  @AssistedInject
  public UpgradeContext(@Assisted Cluster cluster,
      @Assisted Map<String, Object> upgradeRequestMap, Gson gson, UpgradeHelper upgradeHelper,
      UpgradeDAO upgradeDAO, RepositoryVersionDAO repoVersionDAO, ConfigHelper configHelper)
      throws AmbariException {
    // injected constructor dependencies
    m_gson = gson;
    m_upgradeHelper = upgradeHelper;
    m_upgradeDAO = upgradeDAO;
    m_repoVersionDAO = repoVersionDAO;

    m_cluster = cluster;

    // determine upgrade type (default is ROLLING)
    String upgradeTypeProperty = (String) upgradeRequestMap.get(UPGRADE_TYPE);
    if (StringUtils.isNotBlank(upgradeTypeProperty)) {
      try {
        m_type = UpgradeType.valueOf(upgradeRequestMap.get(UPGRADE_TYPE).toString());
      } catch (Exception e) {
        throw new AmbariException(String.format("Property %s has an incorrect value of %s.",
            UPGRADE_TYPE, upgradeTypeProperty));
      }
    } else {
      // default type
      m_type= UpgradeType.ROLLING;
    }

    m_isRevert = upgradeRequestMap.containsKey(UPGRADE_REVERT_UPGRADE_ID);

    if (m_isRevert) {
      Long revertUpgradeId = Long.valueOf(upgradeRequestMap.get(UPGRADE_REVERT_UPGRADE_ID).toString());
      UpgradeEntity revertUpgrade = m_upgradeDAO.findUpgrade(revertUpgradeId);

      if (null == revertUpgrade) {
          throw new AmbariException(String.format("Could not find Upgrade with id %s to revert.", revertUpgradeId));
      }      
      
      if (revertUpgrade.getOrchestration() != RepositoryType.PATCH) {
        throw new AmbariException("Can only revert upgrades that have been done as a patch.");
      }

      if (revertUpgrade.getDirection() != Direction.UPGRADE) {
        throw new AmbariException("Can only revert successful upgrades, not downgrades.");
      }

      Set<RepositoryVersionEntity> priors = new HashSet<>();
      for (UpgradeHistoryEntity history : revertUpgrade.getHistory()) {
        priors.add(history.getFromReposistoryVersion());

        // !!! build all service-specific
        m_services.add(history.getServiceName());
        m_sourceRepositoryMap.put(history.getServiceName(), history.getTargetRepositoryVersion());
        m_targetRepositoryMap.put(history.getServiceName(), history.getFromReposistoryVersion());
      }

      if (priors.size() != 1) {
        String message = String.format("Upgrade from %s could not be reverted as there is no single "
            + " repository across services.", revertUpgrade.getRepositoryVersion().getVersion());

        throw new AmbariException(message);
      }

      m_repositoryVersion = priors.iterator().next();

      // !!! the version is used later in validators
      upgradeRequestMap.put(UPGRADE_REPO_VERSION_ID, m_repositoryVersion.getId().toString());
      // !!! use the same upgrade pack that was used in the upgrade being reverted
      upgradeRequestMap.put(UPGRADE_PACK, revertUpgrade.getUpgradePackage());

      // !!! direction can ONLY be an downgrade on revert
      m_direction = Direction.DOWNGRADE;
      m_orchestration = RepositoryType.PATCH;
    } else {

      // determine direction
      String directionProperty = (String) upgradeRequestMap.get(UPGRADE_DIRECTION);
      if (StringUtils.isEmpty(directionProperty)) {
        throw new AmbariException(String.format("%s is required", UPGRADE_DIRECTION));
      }

      m_direction = Direction.valueOf(directionProperty);

      // depending on the direction, we must either have a target repository or an upgrade we are downgrading from
      switch(m_direction){
        case UPGRADE:{
          String repositoryVersionId = (String) upgradeRequestMap.get(UPGRADE_REPO_VERSION_ID);
          if (null == repositoryVersionId) {
            throw new AmbariException(
                String.format("The property %s is required when the upgrade direction is %s",
                    UPGRADE_REPO_VERSION_ID, m_direction));
          }

          // depending on the repository, add services
          m_repositoryVersion = m_repoVersionDAO.findByPK(Long.valueOf(repositoryVersionId));
          m_orchestration = m_repositoryVersion.getType();

          if (m_orchestration == RepositoryType.STANDARD) {
            m_services.addAll(cluster.getServices().keySet());
          } else {
            try {
              VersionDefinitionXml vdf = m_repositoryVersion.getRepositoryXml();
              m_services.addAll(vdf.getAvailableServiceNames());

              // if this is every true, then just stop the upgrade attempt and
              // throw an exception
              if (m_services.isEmpty()) {
                String message = String.format(
                    "When using a VDF of type %s, the available services must be defined in the VDF",
                    m_repositoryVersion.getType());

                throw new AmbariException(message);
              }

            } catch (Exception e) {
              String msg = String.format(
                  "Could not parse version definition for %s.  Upgrade will not proceed.",
                  m_repositoryVersion.getVersion());

              throw new AmbariException(msg);
            }
          }

          Set<String> installedServices = new HashSet<>();
          // populate the target repository map for all services in the upgrade
          for (String serviceName : m_services) {
            try {
              Service service = cluster.getService(serviceName);
              m_sourceRepositoryMap.put(serviceName, service.getDesiredRepositoryVersion());
              m_targetRepositoryMap.put(serviceName, m_repositoryVersion);
              installedServices.add(serviceName);
            } catch (ServiceNotFoundException e) {
              LOG.warn("Skipping orchestraction for service {}, as it was defined to upgrade, but is not installed in cluster {}",
                  serviceName, cluster.getClusterName());
            }
          }

          m_services = installedServices;

          break;
        }
        case DOWNGRADE:{
          UpgradeEntity upgrade = m_upgradeDAO.findLastUpgradeForCluster(
              cluster.getClusterId(), Direction.UPGRADE);

          m_repositoryVersion = upgrade.getRepositoryVersion();

          // populate the repository maps for all services in the upgrade
          for (UpgradeHistoryEntity history : upgrade.getHistory()) {
            m_services.add(history.getServiceName());
            m_sourceRepositoryMap.put(history.getServiceName(), m_repositoryVersion);
            m_targetRepositoryMap.put(history.getServiceName(), history.getFromReposistoryVersion());
          }

          break;
        }
        default:
          m_repositoryVersion = null;
          break;
      }
    }


    /**
     * For the unit tests tests, there are multiple upgrade packs for the same
     * type, so allow picking one of them. In prod, this is empty.
     */
    String preferredUpgradePackName = (String) upgradeRequestMap.get(UPGRADE_PACK);

    @Experimental(feature = ExperimentalFeature.PATCH_UPGRADES, comment="This is wrong")
    RepositoryVersionEntity upgradeFromRepositoryVersion = cluster.getService(
        m_services.iterator().next()).getDesiredRepositoryVersion();

    m_upgradePack = m_upgradeHelper.suggestUpgradePack(m_cluster.getClusterName(),
        upgradeFromRepositoryVersion.getStackId(), m_repositoryVersion.getStackId(), m_direction,
        m_type, preferredUpgradePackName);

    // the validator will throw an exception if the upgrade request is not valid
    UpgradeRequestValidator upgradeRequestValidator = buildValidator(m_type);
    upgradeRequestValidator.validate(cluster, m_direction, m_type, m_upgradePack,
        upgradeRequestMap);

    // optionally skip failures - this can be supplied on either the request or
    // in the upgrade pack explicitely, however the request will always override
    // the upgrade pack if explicitely specified
    boolean skipComponentFailures = m_upgradePack.isComponentFailureAutoSkipped();
    boolean skipServiceCheckFailures = m_upgradePack.isServiceCheckFailureAutoSkipped();

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
   * Constructor.
   *
   * @param cluster
   *          the cluster that the upgrade is for
   * @param upgradeEntity
   *          the upgrade entity
   */
  @AssistedInject
  public UpgradeContext(@Assisted Cluster cluster, @Assisted UpgradeEntity upgradeEntity,
      AmbariMetaInfo ambariMetaInfo, ConfigHelper configHelper) {
    m_metaInfo = ambariMetaInfo;

    m_cluster = cluster;
    m_type = upgradeEntity.getUpgradeType();
    m_direction = upgradeEntity.getDirection();
    m_repositoryVersion = upgradeEntity.getRepositoryVersion();

    m_autoSkipComponentFailures = upgradeEntity.isComponentFailureAutoSkipped();
    m_autoSkipServiceCheckFailures = upgradeEntity.isServiceCheckFailureAutoSkipped();

    List<UpgradeHistoryEntity> allHistory = upgradeEntity.getHistory();
    for (UpgradeHistoryEntity history : allHistory) {
      String serviceName = history.getServiceName();
      RepositoryVersionEntity sourceRepositoryVersion = history.getFromReposistoryVersion();
      RepositoryVersionEntity targetRepositoryVersion = history.getTargetRepositoryVersion();
      m_sourceRepositoryMap.put(serviceName, sourceRepositoryVersion);
      m_targetRepositoryMap.put(serviceName, targetRepositoryVersion);
      m_services.add(serviceName);
    }

    @Experimental(feature = ExperimentalFeature.PATCH_UPGRADES, comment = "This is wrong")
    String upgradePackage = upgradeEntity.getUpgradePackage();
    StackId stackId = m_repositoryVersion.getStackId();
    Map<String, UpgradePack> packs = m_metaInfo.getUpgradePacks(stackId.getStackName(), stackId.getStackVersion());
    m_upgradePack = packs.get(upgradePackage);

    m_resolver = new MasterHostResolver(m_cluster, configHelper, this);
    m_orchestration = upgradeEntity.getOrchestration();

    m_isRevert = upgradeEntity.getOrchestration() == RepositoryType.PATCH &&
        upgradeEntity.getDirection() == Direction.DOWNGRADE;
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
   * Gets the version that components are being considered to be "coming from".
   * <p/>
   * With a {@link Direction#UPGRADE}, this value represent the services'
   * desired repository. However, {@link Direction#DOWNGRADE} will use the same
   * value for all services which is the version that the downgrade is coming
   * from.
   *
   * @return the source version for the upgrade
   */
  public Map<String, RepositoryVersionEntity> getSourceVersions() {
    return new HashMap<>(m_sourceRepositoryMap);
  }

  /**
   * Gets the version that service is being considered to be "coming from".
   * <p/>
   * With a {@link Direction#UPGRADE}, this value represent the services'
   * desired repository. However, {@link Direction#DOWNGRADE} will use the same
   * value for all services which is the version that the downgrade is coming
   * from.
   *
   * @return the source repository for the upgrade
   */
  public RepositoryVersionEntity getSourceRepositoryVersion(String serviceName) {
    return m_sourceRepositoryMap.get(serviceName);
  }

  /**
   * Gets the version that service is being considered to be "coming from".
   * <p/>
   * With a {@link Direction#UPGRADE}, this value represent the services'
   * desired repository. However, {@link Direction#DOWNGRADE} will use the same
   * value for all services which is the version that the downgrade is coming
   * from.
   *
   * @return the source repository for the upgrade
   * @see #getSourceRepositoryVersion(String)
   */
  public String getSourceVersion(String serviceName) {
    RepositoryVersionEntity serviceSourceVersion = m_sourceRepositoryMap.get(serviceName);
    return serviceSourceVersion.getVersion();
  }

  /**
   * Gets the version being upgraded to or downgraded to for all services
   * participating. This is the version that the service will be on if the
   * upgrade or downgrade succeeds.
   * <p/>
   * With a {@link Direction#UPGRADE}, all services should be targetting the
   * same repository version. However, {@link Direction#DOWNGRADE} will target
   * the original repository that the service was on.
   *
   * @return the target version for the upgrade
   */
  public Map<String, RepositoryVersionEntity> getTargetVersions() {
    return new HashMap<>(m_targetRepositoryMap);
  }

  /**
   * Gets the repository being upgraded to or downgraded to for the given
   * service. This is the version that the service will be on if the upgrade or
   * downgrade succeeds.
   * <p/>
   * With a {@link Direction#UPGRADE}, all services should be targeting the
   * same repository version. However, {@link Direction#DOWNGRADE} will target
   * the original repository that the service was on.
   *
   * @return the target repository for the upgrade
   */
  public RepositoryVersionEntity getTargetRepositoryVersion(String serviceName) {
    return m_targetRepositoryMap.get(serviceName);
  }

  /**
   * Gets the version being upgraded to or downgraded to for the given service.
   * This is the version that the service will be on if the upgrade or downgrade
   * succeeds.
   * <p/>
   * With a {@link Direction#UPGRADE}, all services should be targetting the
   * same repository version. However, {@link Direction#DOWNGRADE} will target
   * the original repository that the service was on.
   *
   * @return the target version for the upgrade
   * @see #getTargetRepositoryVersion(String)
   */
  public String getTargetVersion(String serviceName) {
    RepositoryVersionEntity serviceTargetVersion = m_targetRepositoryMap.get(serviceName);
    return serviceTargetVersion.getVersion();
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
   * Gets the single repository version for the upgrade depending on the
   * direction.
   * <p/>
   * If the direction is {@link Direction#UPGRADE} then this will return the
   * target repository which every service will be on if the upgrade is
   * finalized. <br/>
   * If the direction is {@link Direction#DOWNGRADE} then this will return the
   * repository from which the downgrade is coming from.
   *
   * @return the target repository version for this upgrade (never
   *         {@code null}).
   */
  public RepositoryVersionEntity getRepositoryVersion() {
    return m_repositoryVersion;
  }

  /**
   * @return the service display name, or the service name if not set
   */
  public String getServiceDisplay(String service) {
    if (m_serviceNames.containsKey(service)) {
      return m_serviceNames.get(service);
    }

    return service;
  }

  /**
   * @return the component display name, or the component name if not set
   */
  public String getComponentDisplay(String service, String component) {
    String key = service + ":" + component;
    if (m_componentNames.containsKey(key)) {
      return m_componentNames.get(key);
    }

    return component;
  }

  /**
   * @param service     the service name
   * @param displayName the display name for the service
   */
  public void setServiceDisplay(String service, String displayName) {
    m_serviceNames.put(service, (displayName == null) ? service : displayName);
  }

  /**
   * @param service     the service name that owns the component
   * @param component   the component name
   * @param displayName the display name for the component
   */
  public void setComponentDisplay(String service, String component, String displayName) {
    String key = service + ":" + component;
    m_componentNames.put(key, displayName);
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
   * Gets the services participating in the upgrade.
   *
   * @return the set of supported services. This collection should never be
   *         empty.
   */
  @Experimental(feature=ExperimentalFeature.PATCH_UPGRADES)
  public Set<String> getSupportedServices() {
    return Collections.unmodifiableSet(m_services);
  }

  /**
   * Gets if a service is supported.
   *
   * @param serviceName
   *          the service name to check.
   * @return {@code true} when the service is supported
   */
  @Experimental(feature=ExperimentalFeature.PATCH_UPGRADES)
  public boolean isServiceSupported(String serviceName) {
    return m_services.contains(serviceName);
  }

  @Experimental(feature = ExperimentalFeature.PATCH_UPGRADES)
  public boolean isScoped(UpgradeScope scope) {
    if (scope == UpgradeScope.ANY) {
      return true;
    }

    switch (m_orchestration) {
      case PATCH:
      case SERVICE:
        return scope == UpgradeScope.PARTIAL;
      case STANDARD:
        return scope == UpgradeScope.COMPLETE;
    }

    return false;
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
   * Gets the repository type to determine if this upgrade is a complete upgrade
   * or a service/patch.  This value is not always the same as the repository version.  In
   * the case of a revert of a patch, the target repository may be of type STANDARD, but orchestration
   * must be "like a patch".
   *
   * @return the orchestration type.
   */
  public RepositoryType getOrchestrationType() {
    return m_orchestration;
  }

  /**
   * Gets a map initialized with parameters required for upgrades to work. The
   * following properties are already set:
   * <ul>
   * <li>{@link #COMMAND_PARAM_CLUSTER_NAME}
   * <li>{@link #COMMAND_PARAM_DIRECTION}
   * <li>{@link #COMMAND_PARAM_DOWNGRADE_FROM_VERSION}
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

    if (direction == Direction.DOWNGRADE) {
      parameters.put(COMMAND_PARAM_DOWNGRADE_FROM_VERSION, m_repositoryVersion.getVersion());
    }

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
    return Objects.toStringHelper(this)
        .add("direction", m_direction)
        .add("type", m_type)
        .add("target", m_repositoryVersion).toString();
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
      case NON_ROLLING:
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
        String repositoryVersionId = (String) requestMap.get(UPGRADE_REPO_VERSION_ID);
        if (StringUtils.isBlank(repositoryVersionId)) {
          throw new AmbariException(
              String.format("%s is required for upgrades", UPGRADE_REPO_VERSION_ID));
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

      String repositoryVersionId = (String) requestMap.get(UPGRADE_REPO_VERSION_ID);
      boolean skipPrereqChecks = Boolean.parseBoolean((String) requestMap.get(UPGRADE_SKIP_PREREQUISITE_CHECKS));
      boolean failOnCheckWarnings = Boolean.parseBoolean((String) requestMap.get(UPGRADE_FAIL_ON_CHECK_WARNINGS));
      String preferredUpgradePack = requestMap.containsKey(UPGRADE_PACK) ? (String) requestMap.get(UPGRADE_PACK) : null;

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
          PreUpgradeCheckResourceProvider.UPGRADE_CHECK_FOR_REVERT_PROPERTY_ID).equals(m_isRevert).and().property(
          PreUpgradeCheckResourceProvider.UPGRADE_CHECK_UPGRADE_TYPE_PROPERTY_ID).equals(type).and().property(
          PreUpgradeCheckResourceProvider.UPGRADE_CHECK_UPGRADE_PACK_PROPERTY_ID).equals(preferredUpgradePack).toPredicate();

      Request preUpgradeCheckRequest = PropertyHelper.getReadRequest();

      Set<Resource> preUpgradeCheckResources;
      try {
        preUpgradeCheckResources = provider.getResources(
            preUpgradeCheckRequest, preUpgradeCheckPredicate);
      } catch (NoSuchResourceException|SystemException|UnsupportedPropertyException|NoSuchParentResourceException e) {
        throw new AmbariException(
            String.format("Unable to perform %s. Prerequisite checks could not be run",
                direction.getText(false), e));
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
      List<Grouping> groupings = upgradePack.getGroups(direction);
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
}
