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
package org.apache.ambari.server.state;

import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.VERSION;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.agent.ExecutionCommand.KeyNames;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.stack.MasterHostResolver;
import org.apache.ambari.server.stageplanner.RoleGraphFactory;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.Grouping;
import org.apache.ambari.server.state.stack.upgrade.UpgradeScope;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * Used to hold various helper objects required to process an upgrade pack.
 */
public class UpgradeContext {

  public static final String COMMAND_PARAM_VERSION = VERSION;
  public static final String COMMAND_PARAM_CLUSTER_NAME = "clusterName";
  public static final String COMMAND_PARAM_DIRECTION = "upgrade_direction";
  public static final String COMMAND_PARAM_UPGRADE_PACK = "upgrade_pack";
  public static final String COMMAND_PARAM_REQUEST_ID = "request_id";

  public static final String COMMAND_PARAM_UPGRADE_TYPE = "upgrade_type";
  public static final String COMMAND_PARAM_TASKS = "tasks";
  public static final String COMMAND_PARAM_STRUCT_OUT = "structured_out";
  public static final String COMMAND_DOWNGRADE_FROM_VERSION = "downgrade_from_version";

  /**
   * The original "current" stack of the cluster before the upgrade started.
   * This is the same regardless of whether the current direction is
   * {@link Direction#UPGRADE} or {@link Direction#DOWNGRADE}.
   */
  public static final String COMMAND_PARAM_ORIGINAL_STACK = "original_stack";

  /**
   * The target upgrade stack before the upgrade started. This is the same
   * regardless of whether the current direction is {@link Direction#UPGRADE} or
   * {@link Direction#DOWNGRADE}.
   */
  public static final String COMMAND_PARAM_TARGET_STACK = "target_stack";

  /**
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
   * The request parameters from the REST API for creating this upgrade.
   */
  final private Map<String, Object> m_upgradeRequestMap;

  /**
   * The upgrade pack for this upgrade.
   */
  private UpgradePack m_upgradePack;

  /**
   * The source of the upgrade/downgrade.
   */
  private final RepositoryVersionEntity m_fromRepositoryVersion;

  /**
   * The target of the upgrade/downgrade.
   */
  private final RepositoryVersionEntity m_toRepositoryVersion;


  private MasterHostResolver m_resolver;
  private AmbariMetaInfo m_metaInfo;
  private List<ServiceComponentHost> m_unhealthy = new ArrayList<>();
  private Map<String, String> m_serviceNames = new HashMap<>();
  private Map<String, String> m_componentNames = new HashMap<>();

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

  private Set<String> m_supported = new HashSet<>();

  private UpgradeScope m_scope = UpgradeScope.ANY;

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
   * Constructor.
   *
   * @param cluster
   *          the cluster that the upgrade is for
   * @param type
   *          the type of upgrade, either rolling or non_rolling
   * @param direction
   *          the direction for the upgrade
   * @param upgradeRequestMap
   *          the original map of paramters used to create the upgrade
   */
  @AssistedInject
  public UpgradeContext(@Assisted Cluster cluster, @Assisted UpgradeType type,
      @Assisted Direction direction,
      @Assisted("fromRepositoryVersion") RepositoryVersionEntity fromRepositoryVersion,
      @Assisted("toRepositoryVersion") RepositoryVersionEntity toRepositoryVersion,
      @Assisted Map<String, Object> upgradeRequestMap) {
    m_cluster = cluster;
    m_type = type;
    m_direction = direction;
    m_fromRepositoryVersion = fromRepositoryVersion;
    m_toRepositoryVersion = toRepositoryVersion;
    m_upgradeRequestMap = upgradeRequestMap;
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
      AmbariMetaInfo ambariMetaInfo) {

    m_metaInfo = ambariMetaInfo;
    m_cluster = cluster;
    m_type = upgradeEntity.getUpgradeType();
    m_direction = upgradeEntity.getDirection();

    m_fromRepositoryVersion = upgradeEntity.getFromRepositoryVersion();
    m_toRepositoryVersion = upgradeEntity.getToRepositoryVersion();

    String upgradePackage = upgradeEntity.getUpgradePackage();

    StackId originalStackId = m_fromRepositoryVersion.getStackId();
    if (m_direction == Direction.DOWNGRADE) {
      originalStackId = m_toRepositoryVersion.getStackId();
    }

    Map<String, UpgradePack> packs = m_metaInfo.getUpgradePacks(originalStackId.getStackName(),
        originalStackId.getStackVersion());

    m_upgradePack = packs.get(upgradePackage);

    // since this constructor is initialized from an entity, then this map is
    // not present
    m_upgradeRequestMap = Collections.emptyMap();
  }

  /**
   * Gets the original mapping of key/value pairs from the request which created
   * the upgrade.
   *
   * @return the original mapping of key/value pairs from the request which
   *         created the upgrade.
   */
  public Map<String, Object> getUpgradeRequest() {
    return m_upgradeRequestMap;
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
   * @return the target version for the upgrade
   */
  public RepositoryVersionEntity getTargetRepositoryVersion() {
    return m_toRepositoryVersion;
  }

  /**
   * @return the source version for the upgrade
   */
  public RepositoryVersionEntity getSourceRepositoryVersion() {
    return m_fromRepositoryVersion;
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
   * Sets the host resolver.
   *
   * @param resolver
   *          the resolver that also references the required cluster
   */
  public void setResolver(MasterHostResolver resolver) {
    m_resolver = resolver;
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
   * @param metaInfo the metainfo for access to service definitions
   */
  public void setAmbariMetaInfo(AmbariMetaInfo metaInfo) {
    m_metaInfo = metaInfo;
  }

  /**
   * @param unhealthy a list of unhealthy host components
   */
  public void addUnhealthy(List<ServiceComponentHost> unhealthy) {
    m_unhealthy.addAll(unhealthy);
  }

  /**
   * Gets the target stack of the upgrade based on the target repository which
   * finalization will set.
   *
   * @return the target stack (never {@code null}).
   */
  public StackId getTargetStackId() {
    return m_toRepositoryVersion.getStackId();
  }

  /**
   * Gets the current stack of the components participating in the upgrade.
   *
   * @return the source stack (never {@code null}).
   */
  public StackId getSourceStackId() {
    return m_fromRepositoryVersion.getStackId();
  }

  /**
   * Gets the original stack ID that the cluster was on. For an upgrade, this
   * returns the source stack ID. For a downgrade, this will return the target
   * stack ID.
   *
   * @return the original stack ID.
   */
  public StackId getOriginalStackId() {
    StackId originalStackId = getSourceStackId();
    if (m_direction == Direction.DOWNGRADE) {
      originalStackId = getTargetStackId();
    }

    return originalStackId;
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
   * Sets whether skippable components that failed are automatically skipped.
   *
   * @param autoSkipComponentFailures
   *          {@code true} to automatically skip component failures which are
   *          marked as skippable.
   */
  public void setAutoSkipComponentFailures(boolean autoSkipComponentFailures) {
    m_autoSkipComponentFailures = autoSkipComponentFailures;
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
   * Sets whether skippable service checks that failed are automatically
   * skipped.
   *
   * @param autoSkipServiceCheckFailures
   *          {@code true} to automatically skip service check failures which
   *          are marked as being skippable.
   */
  public void setAutoSkipServiceCheckFailures(boolean autoSkipServiceCheckFailures) {
    m_autoSkipServiceCheckFailures = autoSkipServiceCheckFailures;
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
   * Sets whether manual verification checks are automatically skipped.
   *
   * @param autoSkipManualVerification
   *          {@code true} to automatically skip manual verification tasks.
   */
  public void setAutoSkipManualVerification(boolean autoSkipManualVerification) {
    m_autoSkipManualVerification = autoSkipManualVerification;
  }

  /**
   * Sets the service names that are supported by an upgrade.  This is used for
   * {@link RepositoryType#PATCH} and {@link RepositoryType#SERVICE}.
   *
   * @param services  the set of specific services
   */
  @Experimental(feature=ExperimentalFeature.PATCH_UPGRADES)
  public void setSupportedServices(Set<String> services) {
    m_supported = services;
  }

  /**
   * Gets if a service is supported.  If there are no services marked for the context,
   * then ALL services are supported
   * @param serviceName the service name to check.
   * @return {@code true} when the service is supported
   */
  @Experimental(feature=ExperimentalFeature.PATCH_UPGRADES)
  public boolean isServiceSupported(String serviceName) {
    if (m_supported.isEmpty() || m_supported.contains(serviceName)) {
      return true;
    }

    return false;
  }

  @Experimental(feature=ExperimentalFeature.PATCH_UPGRADES)
  public void setScope(UpgradeScope scope) {
    m_scope = scope;
  }

  @Experimental(feature=ExperimentalFeature.PATCH_UPGRADES)
  public boolean isScoped(UpgradeScope scope) {
    return m_scope.isScoped(scope);
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

    parameters.put(COMMAND_PARAM_CLUSTER_NAME, m_cluster.getClusterName());
    parameters.put(COMMAND_PARAM_DIRECTION, getDirection().name().toLowerCase());

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
}
