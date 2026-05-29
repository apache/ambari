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

import { ambariApi } from "./config/axiosConfig";
const VersionsApi = {
  versionsList: async function (repoVersion: string, clusterName: string) {
    const url = `/clusters/${clusterName}/stack_versions?fields=*&ClusterStackVersions/repository_version=${repoVersion}`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getRepos: async function () {
    const url = `/stacks?fields=versions/repository_versions/RepositoryVersions`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getClusterInfo: async function () {
    const url = `/clusters?fields=Clusters/cluster_id`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getVersionDefinitions: async function () {
    const url = `/version_definitions?fields=VersionDefinition/stack_default,VersionDefinition/stack_repo_update_link_exists,VersionDefinition/max_jdk,VersionDefinition/min_jdk,operating_systems/repositories/Repositories/*,operating_systems/OperatingSystems/*,VersionDefinition/stack_services,VersionDefinition/repository_version&VersionDefinition/show_available=true&VersionDefinition/stack_name=VDP&_=1727429673209`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getAllVersionDefinitions: async function () {
    const url = `/version_definitions`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getVersionOperatingSystems: async function (versionName: string) {
    const url = `/stacks/VDP/versions/${versionName}?fields=operating_systems/repositories/Repositories`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  readVersionInfo: async function (
    payload: any,
    headers = {},
    isDryRun = true
  ) {
    const url = `/version_definitions?skip_url_check=true${
      isDryRun ? "&dry_run=true" : ""
    }`;
    const response = await ambariApi.request({
      url: url,
      method: "POST",
      headers: { ...headers },
      data: payload,
    });
    return response.data;
  },
  validateRepos: async function name(
    stack: string,
    stackVersion: string,
    os: string,
    repo: string,
    payload: { base_url: string; repo_name: string }
  ) {
    const url = `/stacks/${stack}/versions/${stackVersion}/operating_systems/${os}/repositories/${repo}?validate_only=true`;
    const response = await ambariApi.request({
      url: url,
      method: "POST",
      data: { Repositories: payload },
    });
    return response.data;
  },
  saveRepoVersions: async function name(
    stack: string,
    stackVersion: string,
    version_id: string,
    payload: any
  ) {
    const url = `/stacks/${stack}/versions/${stackVersion}/repository_versions/${version_id}`;
    const response = await ambariApi.request({
      url: url,
      method: "PUT",
      data: payload,
    });
    return response.data;
  },
  getRepoDetails: async (stack: string, version: string) => {
    const url = `/stacks/${stack}/versions?fields=repository_versions/operating_systems/repositories/*,repository_versions/operating_systems/OperatingSystems/*,repository_versions/RepositoryVersions/*&repository_versions/RepositoryVersions/repository_version=${version}`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  deleteRepositoryVersion: async function deleteRepositoryVersion(
    stack: string,
    stackVersion: string,
    repositoryVersion: string
  ) {
    const url = `/stacks/${stack}/versions/${stackVersion}/repository_versions/${repositoryVersion}`;
    const response = await ambariApi.request({
      url: url,
      method: "DELETE",
    });
    return response.data;
  },
  postVersionDefinitionFile: async function postVersionDefinitionFile(
    data: any
  ) {
    const url = `/version_definitions`;
    const response = await ambariApi.request({
      url,
      method: "POST",
      data,
    });
    return response.data;
  },
  getVersionDefinition: async function getVersionDefinition() {
    const url = `/version_definitions`;
    const response = await ambariApi.request({
      url,
      method: "GET",
    });
    return response.data;
  },
  updateRepoOSInfo: async function updateRepoOSInfo(
    stackName: string,
    stackVersion: string,
    repoVersionId: string,
    data: any
  ) {
    {
      const url = `/stacks/${stackName}/versions/${stackVersion}/repository_versions/${repoVersionId}`;
      const response = await ambariApi.request({
        url,
        method: "PUT",
        data,
      });
      return response.data;
    }
  },
  updateOSInfo: async function updateRepoOSInfo(
    stackName: string,
    stackVersion: string,
    operatingSystem: string,
    repoVersionId: string,
    data: any
  ) {
    {
      const url = `/stacks/${stackName}/versions/${stackVersion}/operating_systems/${operatingSystem}/repositories/${repoVersionId}`;
      const response = await ambariApi.request({
        url,
        method: "PUT",
        data,
      });
      return response.data;
    }
  },
  getServices: async function (clusterName: string) {
    const url = `/clusters/${clusterName}/stack_versions?fields=*,repository_versions/*`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getAllStacks: async function (clusterName: string) {
    const url = `/clusters/${clusterName}/stack_versions?fields=*,repository_versions/*,repository_versions/operating_systems/OperatingSystems/*,repository_versions/operating_systems/repositories/*`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  }, 
  get_supported_upgradeTypes: async function (
    stackName: string,
    stackVersion: string,
    toVersion: string
  ) {
    const url = `/stacks/${stackName}/versions/${stackVersion}/compatible_repository_versions?CompatibleRepositoryVersions/repository_version=${toVersion}`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  runPreUpgradeCheck: async function (
    clusterName: string,
    toId: string,
    upgradeType: string
  ) {
    const url = `/clusters/${clusterName}/rolling_upgrades_check?fields=*&UpgradeChecks/repository_version_id=${toId}&UpgradeChecks/upgrade_type=${upgradeType}`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getUpgradeId: async function (data: any, clusterName: string) {
    const url = `/clusters/${clusterName}/upgrades`;
    const response = await ambariApi.request({
      url,
      method: "POST",
      data,
    });
    return response.data;
  },
  getUpgradeOperations: async function (id: number, clusterName: string) {
    const url = `/clusters/${clusterName}/upgrades/${id}?upgrade_groups/UpgradeGroup/status!=PENDING&fields=Upgrade/progress_percent,Upgrade/request_context,Upgrade/request_status,Upgrade/direction,Upgrade/downgrade_allowed,upgrade_groups/UpgradeGroup,Upgrade/*,upgrade_groups/upgrade_items/UpgradeItem/status,upgrade_groups/upgrade_items/UpgradeItem/display_status,upgrade_groups/upgrade_items/UpgradeItem/context,upgrade_groups/upgrade_items/UpgradeItem/group_id,upgrade_groups/upgrade_items/UpgradeItem/progress_percent,upgrade_groups/upgrade_items/UpgradeItem/request_id,upgrade_groups/upgrade_items/UpgradeItem/skippable,upgrade_groups/upgrade_items/UpgradeItem/stage_id,upgrade_groups/upgrade_items/UpgradeItem/text&minimal_response=true`;
    const response = await ambariApi.request({
      url,
      method: "GET",
    });
    return response.data;
  },
  getTasksList: async function (
    upgradeId: number,
    groupId: number,
    stageId: number,
    clusterName: string
  ) {
    const url = `/clusters/${clusterName}/upgrades/${upgradeId}/upgrade_groups/${groupId}/upgrade_items/${stageId}?fields=UpgradeItem/group_id,UpgradeItem/stage_id,tasks/Tasks/command_detail,tasks/Tasks/host_name,tasks/Tasks/role,tasks/Tasks/request_id,tasks/Tasks/stage_id,tasks/Tasks/status,tasks/Tasks/structured_out&minimal_response=true`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getTasksLogs: async function (
    upgradeId: number,
    groupId: number,
    stageId: number,
    taskId: number,
    clusterName: string
  ) {
    const url = `/clusters/${clusterName}/upgrades/${upgradeId}/upgrade_groups/${groupId}/upgrade_items/${stageId}/tasks/${taskId}`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  setUpgradeItemState: async function (clusterName: string, data: any) {
    const url = `/clusters/${clusterName}/upgrades/${data.upgradeId}/upgrade_groups/${data.groupId}/upgrade_items/${data.itemId}`;
    const response = await ambariApi.request({
      url,
      method: "PUT",
      data: JSON.stringify({
        UpgradeItem: {
          status: data.status,
        },
      }),
    });
    return response.data;
  },
  getUpgradeItem: async function (
    upgradeId: number,
    groupId: number,
    stageId: number,
    clusterName: string
  ) {
    const url =
      `/clusters/${clusterName}/upgrades/${upgradeId}/upgrade_groups/${groupId}/upgrade_items/${stageId}?fields=` +
      [
        "UpgradeItem/group_id",
        "UpgradeItem/stage_id",
        "tasks/Tasks/command_detail",
        "tasks/Tasks/host_name",
        "tasks/Tasks/role",
        "tasks/Tasks/request_id",
        "tasks/Tasks/stage_id",
        "tasks/Tasks/status",
        "tasks/Tasks/structured_out",
      ].join(",") +
      "&minimal_response=true";
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  abortUpgrade: async function (clusterName: string, upgradeId: number) {
    const url = `/clusters/${clusterName}/upgrades/${upgradeId}`;
    const response = await ambariApi.request({
      url,
      method: "PUT",
      data: JSON.stringify({
        "Upgrade": {
          "request_status": "ABORTED",
          "suspended": "false"
        },
      }),
    });
    return response.data;
  },
  suspendUpgrade: async function (clusterName: string, upgradeId: number) {
    const url = `/clusters/${clusterName}/upgrades/${upgradeId}`;
    const response = await ambariApi.request({
      url,
      method: "PUT",
      data: JSON.stringify({
        "Upgrade": {
          "request_status": "ABORTED",
          "suspended": "true"
        },
      }),
    });
    return response.data;
  },
  retryUpgrade: async function (clusterName: string, upgradeId: number) {
    const url = `/clusters/${clusterName}/upgrades/${upgradeId}`;
    const response = await ambariApi.request({
      url,
      method: "PUT",
      data: JSON.stringify({
        "Upgrade": {
          "request_status": "PENDING",
        },
      }),
    });
    return response.data; 
  },
  getUpgradeHistory: async function (clusterName: string) {
    const url = `/clusters/${clusterName}/upgrades?fields=Upgrade`;
    const response = await ambariApi.request({
        url,
        method: "GET"
    });
    return response.data;
  },
  updateUpgrade: async function (upgradeId: number, data: any, clusterName: string) {
    const url = `/clusters/${clusterName}/upgrades/${upgradeId}`;
    const response = await ambariApi.request({
      url,
      method: "PUT",
      data,
    });
    return response.data;
  }
};

export default VersionsApi;
