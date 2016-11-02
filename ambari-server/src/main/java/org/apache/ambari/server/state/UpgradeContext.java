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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.stack.MasterHostResolver;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.UpgradeScope;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;

/**
 * Used to hold various helper objects required to process an upgrade pack.
 */
public class UpgradeContext {

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
   * The version being upgrade to or downgraded to.
   */
  private String m_version;

  /**
   * The original "current" stack of the cluster before the upgrade started.
   * This is the same regardless of whether the current direction is
   * {@link Direction#UPGRADE} or {@link Direction#DOWNGRADE}.
   */
  private StackId m_originalStackId;

  /**
   * The stack currently used to start/restart services during an upgrade.This is the same
   * During a {@link UpgradeType#ROLLING} upgrade, this is always the {@link this.m_targetStackId},
   * During a {@link UpgradeType#NON_ROLLING} upgrade, this is initially the {@link this.m_sourceStackId} while
   * stopping services, and then changes to the {@link this.m_targetStackId} when starting services.
   */
  private StackId m_effectiveStackId;

  /**
   * The target upgrade stack before the upgrade started. This is the same
   * regardless of whether the current direction is {@link Direction#UPGRADE} or
   * {@link Direction#DOWNGRADE}.
   */
  private StackId m_targetStackId;

  private MasterHostResolver m_resolver;
  private AmbariMetaInfo m_metaInfo;
  private List<ServiceComponentHost> m_unhealthy = new ArrayList<ServiceComponentHost>();
  private Map<String, String> m_serviceNames = new HashMap<String, String>();
  private Map<String, String> m_componentNames = new HashMap<String, String>();
  private String m_downgradeFromVersion = null;

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
  public UpgradeContext(Cluster cluster, UpgradeType type, Direction direction,
      Map<String, Object> upgradeRequestMap) {
    m_cluster = cluster;
    m_type = type;
    m_direction = direction;
    m_upgradeRequestMap = upgradeRequestMap;
  }

  /**
   * Sets the source and target stack IDs. This will also set the effective
   * stack ID based on the already-set {@link UpgradeType} and
   * {@link Direction}.
   *
   * @param sourceStackId
   *          the original "current" stack of the cluster before the upgrade
   *          started. This is the same regardless of whether the current
   *          direction is {@link Direction#UPGRADE} or
   *          {@link Direction#DOWNGRADE} (not {@code null}).
   * @param targetStackId
   *          The target upgrade stack before the upgrade started. This is the
   *          same regardless of whether the current direction is
   *          {@link Direction#UPGRADE} or {@link Direction#DOWNGRADE} (not
   *          {@code null}).
   *
   * @see #getEffectiveStackId()
   */
  public void setSourceAndTargetStacks(StackId sourceStackId, StackId targetStackId) {
    m_originalStackId = sourceStackId;

    switch (m_type) {
      case ROLLING:
        m_effectiveStackId = targetStackId;
        break;
      case NON_ROLLING:
        m_effectiveStackId = (m_direction.isUpgrade()) ? sourceStackId : targetStackId;
        break;
      default:
        m_effectiveStackId = targetStackId;
        break;
    }

    m_targetStackId = targetStackId;
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
  public String getVersion() {
    return m_version;
  }

  /**
   * @param version
   *          the target version to upgrade to
   */
  public void setVersion(String version) {
    m_version = version;
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
   * @return the originalStackId
   */
  public StackId getOriginalStackId() {
    return m_originalStackId;
  }

  /**
   * @param originalStackId
   *          the originalStackId to set
   */
  public void setOriginalStackId(StackId originalStackId) {
    m_originalStackId = originalStackId;
  }

  /**
   * @return the effectiveStackId that is currently in use.
   */
  public StackId getEffectiveStackId() {
    return m_effectiveStackId;
  }

  /**
   * @param effectiveStackId the effectiveStackId to set
   */
  public void setEffectiveStackId(StackId effectiveStackId) {
    m_effectiveStackId = effectiveStackId;
  }


  /**
   * @return the targetStackId
   */
  public StackId getTargetStackId() {
    return m_targetStackId;
  }

  /**
   * @param targetStackId
   *          the targetStackId to set
   */
  public void setTargetStackId(StackId targetStackId) {
    m_targetStackId = targetStackId;
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
   * This method returns the non-finalized version we are downgrading from.
   *
   * @return version cluster is downgrading from
   */
  public String getDowngradeFromVersion() {
    return m_downgradeFromVersion;
  }

  /**
   * Set the HDP stack version we are downgrading from.
   *
   * @param downgradeFromVersion
   */
  public void setDowngradeFromVersion(String downgradeFromVersion) {
    m_downgradeFromVersion = downgradeFromVersion;
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
}
