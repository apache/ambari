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

export const hostMapper = {
  hostConfig: {
    hostName: "Hosts.host_name",
    publicHostName: "Hosts.public_host_name",
    cluster: "Hosts.cluster_name",
    cpu: "Hosts.cpu_count",
    cpuPhysical: "Hosts.ph_cpu_count",
    memory: "Hosts.total_mem",
    osArch: "Hosts.os_arch",
    ip: "Hosts.ip",
    rack: "Hosts.rack_info",
    healthStatus: "Hosts.host_status",
    state: "Hosts.host_state",
    lastHeartBeatTime: "Hosts.last_heartbeat_time",
    hasJcePolicy: "Hosts.last_agent_env.hasUnlimitedJcePolicy",
    osType: "Hosts.os_type",
    diskInfo: "Hosts.disk_info",
    alertsSummary: "alerts_summary",
    passiveState: "Hosts.maintenance_state",
  },
  hostComponentConfig: {
    workStatus: "HostRoles.state",
    passiveState: "HostRoles.maintenance_state",
    componentName: "HostRoles.component_name",
    displayName: "HostRoles.display_name",
    staleConfigs: "HostRoles.stale_configs",
    hostName: "HostRoles.host_name",
    adminState: "HostRoles.desired_admin_state",
    nnHAState: "metrics.dfs.FSNamesystem.HAState",
    cardinality: "HostRoles.cardinality",
    customCommands: "HostRoles.custom_commands",
    reassignAllowed: "HostRoles.reassign_allowed",
    decommissionAllowed: "HostRoles.decommission_allowed",
    hasBulkCommandsDefinition: "HostRoles.has_bulk_commands_definition",
    bulkCommandsDisplayName: "HostRoles.bulk_commands_display_name",
    bulkCommandsMasterComponentName:
      "HostRoles.bulk_commands_master_component_name",
    dependencies: "HostRoles.dependencies",
    serviceName: "HostRoles.service_name",
    componentCategory: "HostRoles.component_category",
    rollingRestartSupported: "HostRoles.rolling_restart_supported",
    isMaster: "HostRoles.is_master",
    isClient: "HostRoles.is_client",
    componentType: "HostRoles.component_type",
    stackName: "HostRoles.stack_name",
    stackVersion: "HostRoles.stack_version",
    recoveryEnabled: "HostRoles.recovery_enabled",
    advertiseVersion: "HostRoles.advertise_version",
    clusterName: "HostRoles.cluster_name",
  },
  hostStackVersionConfig: {
    stack: "HostStackVersions.stack",
    version: "HostStackVersions.version",
    repo: "repository_versions[0].RepositoryVersions",
    repoVersion: "repository_versions[0].RepositoryVersions.repository_version",
    displayName: "repository_versions[0].RepositoryVersions.display_name",
    isVisible: "is_visible",
    status: "HostStackVersions.state",
    hostName: "HostStackVersions.host_name",
  },
  hostComponentLogConfig: {
    name: "logging.name",
    hostName: "HostRoles.host_name",
    serviceName: "HostRoles.service_name",
  }
};
