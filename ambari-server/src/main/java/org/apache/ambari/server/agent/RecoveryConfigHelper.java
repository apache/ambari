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

package org.apache.ambari.server.agent;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class RecoveryConfigHelper {

  /**
   * Recovery related configuration
   */
  public static final String RECOVERY_ENABLED_KEY = "recovery_enabled";
  public static final String RECOVERY_TYPE_KEY = "recovery_type";
  public static final String RECOVERY_TYPE_DEFAULT = "AUTO_START";
  public static final String RECOVERY_LIFETIME_MAX_COUNT_KEY = "recovery_lifetime_max_count";
  public static final String RECOVERY_LIFETIME_MAX_COUNT_DEFAULT = "12";
  public static final String RECOVERY_MAX_COUNT_KEY = "recovery_max_count";
  public static final String RECOVERY_MAX_COUNT_DEFAULT = "6";
  public static final String RECOVERY_WINDOW_IN_MIN_KEY = "recovery_window_in_minutes";
  public static final String RECOVERY_WINDOW_IN_MIN_DEFAULT = "60";
  public static final String RECOVERY_RETRY_GAP_KEY = "recovery_retry_interval";
  public static final String RECOVERY_RETRY_GAP_DEFAULT = "5";

  @Inject
  private Clusters clusters;

  private Cluster cluster;
  private Map<String, String> configProperties;

  public RecoveryConfigHelper() {
  }

  public RecoveryConfig getDefaultRecoveryConfig()
      throws AmbariException {
      return getRecoveryConfig(null, null);
  }

  public RecoveryConfig getRecoveryConfig(String clusterName, String hostname)
      throws AmbariException {

    if (StringUtils.isNotEmpty(clusterName)) {
      cluster = clusters.getCluster(clusterName);
    }

    configProperties = null;

    if (cluster != null) {
      Config config = cluster.getDesiredConfigByType(getConfigType());
      if (config != null) {
        configProperties = config.getProperties();
      }
    }

    if (configProperties == null) {
      configProperties = new HashMap<>();
    }

    RecoveryConfig recoveryConfig = new RecoveryConfig();
    recoveryConfig.setMaxCount(getNodeRecoveryMaxCount());
    recoveryConfig.setMaxLifetimeCount(getNodeRecoveryLifetimeMaxCount());
    recoveryConfig.setRetryGap(getNodeRecoveryRetryGap());
    recoveryConfig.setType(getNodeRecoveryType());
    recoveryConfig.setWindowInMinutes(getNodeRecoveryWindowInMin());
    if (isRecoveryEnabled()) {
      recoveryConfig.setEnabledComponents(StringUtils.join(getEnabledComponents(hostname), ','));
    }

    return recoveryConfig;
  }

  /**
   * Get a list of enabled components for the specified host and cluster. Filter by
   * Maintenance Mode OFF, so that agent does not auto start components that are in
   * maintenance mode.
   * @return
   */
  private List<String> getEnabledComponents(String hostname) {
    List<String> enabledComponents = new ArrayList<>();
    List<ServiceComponentHost> scHosts = cluster.getServiceComponentHosts(hostname);

    for (ServiceComponentHost sch : scHosts) {
      if (sch.isRecoveryEnabled()) {
        // Keep the components that are not in maintenance mode.
        if (sch.getMaintenanceState() == MaintenanceState.OFF) {
          enabledComponents.add(sch.getServiceComponentName());
        }
      }
    }

    return enabledComponents;
  }

  /**
   * The configuration type name.
   * @return
   */
  private String getConfigType() {
    return "cluster-env";
  }

  /**
   * Get a value indicating whether the cluster supports recovery.
   *
   * @return True or false.
   */
  private boolean isRecoveryEnabled() {
    return Boolean.parseBoolean(getProperty(RECOVERY_ENABLED_KEY, "false"));
  }

  /**
   * Get the node recovery type. The only supported value is AUTO_START.
   * @return
   */
  private String getNodeRecoveryType() {
    return getProperty(RECOVERY_TYPE_KEY, RECOVERY_TYPE_DEFAULT);
  }

  /**
   * Get configured max count of recovery attempt allowed per host component in a window
   * This is reset when agent is restarted.
   * @return
   */
  private String getNodeRecoveryMaxCount() {
    return getProperty(RECOVERY_MAX_COUNT_KEY, RECOVERY_MAX_COUNT_DEFAULT);
  }

  /**
   * Get configured max lifetime count of recovery attempt allowed per host component.
   * This is reset when agent is restarted.
   * @return
   */
  private String getNodeRecoveryLifetimeMaxCount() {
    return getProperty(RECOVERY_LIFETIME_MAX_COUNT_KEY, RECOVERY_LIFETIME_MAX_COUNT_DEFAULT);
  }

  /**
   * Get configured window size in minutes
   * @return
   */
  private String getNodeRecoveryWindowInMin() {
    return getProperty(RECOVERY_WINDOW_IN_MIN_KEY, RECOVERY_WINDOW_IN_MIN_DEFAULT);
  }

  /**
   * Get the configured retry gap between tries per host component
   * @return
   */
  private String getNodeRecoveryRetryGap() {
    return getProperty(RECOVERY_RETRY_GAP_KEY, RECOVERY_RETRY_GAP_DEFAULT);
  }

  /**
   * Get the property value for the specified key. If not present, return default value.
   * @param key The key for which property value is required.
   * @param defaultValue Default value to return if key is not found.
   * @return
   */
  private String getProperty(String key, String defaultValue) {
    if (configProperties.containsKey(key)) {
      return configProperties.get(key);
    }

    return defaultValue;
  }
}
