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
import { adminApi } from "./configs/axiosConfig";
const VersionsApi = {
  versionsList: async function (repoVersion: string,clusterName:string) {
    const url = `/clusters/${clusterName}/stack_versions?fields=*&ClusterStackVersions/repository_version=${repoVersion}`;
    const response = await adminApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getRepos: async function () {
    const url = `/stacks?fields=versions/repository_versions/RepositoryVersions`;
    const response = await adminApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getClusterInfo: async function () {
    const url = `/clusters?fields=Clusters/cluster_id`;
    const response = await adminApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getVersionDefinitions: async function () {
    const url = `/version_definitions?fields=VersionDefinition/stack_default,VersionDefinition/type,VersionDefinition/stack_repo_update_link_exists,operating_systems/repositories/Repositories/*,VersionDefinition/stack_services,VersionDefinition/repository_version&VersionDefinition/show_available=true`;
    const response = await adminApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getVersionOperatingSystems: async function (versionName: string) {
    const url = `/stacks/VDP/versions/${versionName}?fields=operating_systems/repositories/Repositories`;
    const response = await adminApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  readVersionInfo: async function (payload: any,headers={},isDryRun=true) {
    const url = `/version_definitions?skip_url_check=true${isDryRun?"&dry_run=true":""}`;
    const response = await adminApi.request({
      url: url,
      method: "POST",
      headers:{...headers},
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
    const response = await adminApi.request({
      url: url,
      method: "POST",
      data: { Repositories: payload },
    });
    return response.data;
  },
  saveRepoVersions:async function name(
    stack: string,
    stackVersion: string,
    version_id:string,
    payload:any
  ) {
    const url = `/stacks/${stack}/versions/${stackVersion}/repository_versions/${version_id}`;
    const response = await adminApi.request({
      url: url,
      method: "PUT",
      data: payload,
    });
    return response.data;
  },
  getRepoDetails:async(stack:string,version:string)=>{
    const url = `/stacks/${stack}/versions?fields=repository_versions/operating_systems/repositories/*,repository_versions/operating_systems/OperatingSystems/*,repository_versions/RepositoryVersions/*&repository_versions/RepositoryVersions/repository_version=${version}`;
    const response = await adminApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  deleteRepositoryVersion:async function deleteRepositoryVersion(stack:string,stackVersion:string,repositoryVersion:string){
    const url = `/stacks/${stack}/versions/${stackVersion}/repository_versions/${repositoryVersion}`;
    const response = await adminApi.request({
      url: url,
      method: "DELETE",
    });
    return response.data;
  }
};

export default VersionsApi;