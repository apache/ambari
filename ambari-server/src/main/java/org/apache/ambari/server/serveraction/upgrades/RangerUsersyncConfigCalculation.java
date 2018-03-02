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

package org.apache.ambari.server.serveraction.upgrades;

import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.Host;

/**
 * Computes Ranger Usersync ldap grouphierarchylevels property. This class is only used when upgrading from
 * HDP-2.6.x to HDP-2.6.y.
 */

public class RangerUsersyncConfigCalculation extends AbstractUpgradeServerAction {
  private static final String RANGER_USERSYNC_CONFIG_TYPE = "ranger-ugsync-site";
  private static final String RANGER_ENV_CONFIG_TYPE = "ranger-env";

  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext) throws AmbariException, InterruptedException {

  String clusterName = getExecutionCommand().getClusterName();
  Cluster cluster = getClusters().getCluster(clusterName);
  String outputMsg = "";

  Config rangerUsersyncConfig = cluster.getDesiredConfigByType(RANGER_USERSYNC_CONFIG_TYPE);

  if (null == rangerUsersyncConfig) {
    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
      MessageFormat.format("Config type {0} not found, skipping updating property in same.", RANGER_USERSYNC_CONFIG_TYPE), "");
  }

  String ldapGroupHierarchy = "0";

  if (rangerUsersyncConfig.getProperties().containsKey("ranger.usersync.ldap.grouphierarchylevels")) {
    ldapGroupHierarchy = rangerUsersyncConfig.getProperties().get("ranger.usersync.ldap.grouphierarchylevels");
  } else {
    Map<String, String> targetRangerUsersyncConfig = rangerUsersyncConfig.getProperties();
    targetRangerUsersyncConfig.put("ranger.usersync.ldap.grouphierarchylevels", ldapGroupHierarchy);
    rangerUsersyncConfig.setProperties(targetRangerUsersyncConfig);
    rangerUsersyncConfig.save();

    outputMsg = outputMsg + MessageFormat.format("Successfully updated {0} config type.\n", RANGER_USERSYNC_CONFIG_TYPE);
  }

  Config rangerEnvConfig = cluster.getDesiredConfigByType(RANGER_ENV_CONFIG_TYPE);

  if (null == rangerEnvConfig) {
    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}",
      MessageFormat.format("Config type {0} not found, skipping updating property in same.", RANGER_ENV_CONFIG_TYPE), "");
  }

  String enableSyncNestedGroup = "false";

  if (!ldapGroupHierarchy.equals("0") ) {
    enableSyncNestedGroup = "true";
  }

  Map<String, String> targetRangerEnvConfig = rangerEnvConfig.getProperties();
  targetRangerEnvConfig.put("is_nested_groupsync_enabled", enableSyncNestedGroup);
  rangerEnvConfig.setProperties(targetRangerEnvConfig);
  rangerEnvConfig.save();
  agentConfigsHolder.updateData(cluster.getClusterId(), cluster.getHosts().stream().map(Host::getHostId).collect(Collectors.toList()));

  outputMsg = outputMsg + MessageFormat.format("Successfully updated {0} config type.\n", RANGER_ENV_CONFIG_TYPE);

  return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", outputMsg, "");
  }
}
