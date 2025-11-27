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

import { useContext, useEffect, useState } from "react";
import VersionsApi from "../api/VersionsApi";
import { forEach, get } from "lodash";
import { AppContext } from "../store/context";

const useStackVersion = () => {
  const { clusterName } = useContext(AppContext);
  const [stackVersion, setStackVersion] = useState();
  const [stackVersionList, setStackVersionList] = useState([]);

  const getStackVersion = async () => {
    const response = await VersionsApi.getServices(clusterName);
    setStackVersion(response);
  };

  useEffect(() => {
    getStackVersion();
  }, []);

  useEffect(() => {
    if (stackVersion) {
      let stackVersionListCopy: any = [];
      forEach(get(stackVersion, "items", []), (item: any) => {
        const repoVersionId = get(
          item,
          "ClusterStackVersions.repository_version"
        );
        const repoVersion = get(item, "repository_versions", []).find(
          (repo: any) => get(repo, "RepositoryVersions.id") === repoVersionId
        )?.RepositoryVersions?.repository_version;
        stackVersionListCopy.push({
          id: get(item, "ClusterStackVersions.id"),
          cluster_name: get(item, "ClusterStackVersions.cluster_name"),
          stack: get(item, "ClusterStackVersions.stack"),
          version: get(item, "ClusterStackVersions.version"),
          state: get(item, "ClusterStackVersions.state"),
          displayName: get(item, "repository_versions.[0].RepositoryVersions.display_name"),
          not_installed_hosts: get(
            item,
            "ClusterStackVersions.host_states.NOT_REQUIRED"
          ),
          installing_hosts: get(
            item,
            "ClusterStackVersions.host_states.INSTALLING"
          ),
          installed_hosts: get(
            item,
            "ClusterStackVersions.host_states.INSTALLED"
          ),
          install_failed_hosts: get(
            item,
            "ClusterStackVersions.host_states.INSTALL_FAILED"
          ),
          out_of_sync_hosts: get(
            item,
            "ClusterStackVersions.host_states.OUT_OF_SYNC"
          ),
          current_hosts: get(item, "ClusterStackVersions.host_states.CURRENT"),
          supports_revert: get(item, "ClusterStackVersions.supports_revert"),
          repository_version_id: repoVersionId,
          repository_version: repoVersion,
        });
      });
      setStackVersionList(stackVersionListCopy);
    }
  }, [stackVersion]);

  return { stackVersion, stackVersionList };
};

export default useStackVersion;
