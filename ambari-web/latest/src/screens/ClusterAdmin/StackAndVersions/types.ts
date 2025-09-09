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

type RepositoryVersion = {
  href: string;
  RepositoryVersions: {
    display_name: string;
    has_children: boolean;
    hidden: boolean;
    id: number;
    parent_id: number | null;
    repository_version: string;
    resolved: boolean;
    services: Service[];
    stack_name: string;
    stack_services: StackService[];
    stack_version: string;
    type: string;
    release: {
      build: string | null;
      compatible_with: string | null;
      notes: string;
      version: string;
    };
  };
  operating_systems: OperatingSystem[];
}

type Service = {
  name: string;
  versions: Version[];
  display_name: string;
}

type Version = {
  version: string;
  components: any[];
}

type StackService = {
  name: string;
  display_name: string;
  comment: string;
  versions: string[];
}

type OperatingSystem = {
  href: string;
  OperatingSystems: {
    ambari_managed_repositories: boolean;
    os_type: string;
    repository_version_id: number;
    stack_name: string;
    stack_version: string;
  };
  repositories: Repository[];
}

type Repository = {
  href: string;
  Repositories: {
    applicable_services: any[];
    base_url: string;
    cluster_version_id: number;
    components: any | null;
    default_base_url: string;
    distribution: any | null;
    mirrors_list: string;
    os_type: string;
    repo_id: string;
    repo_name: string;
    repository_version_id: number;
    stack_name: string;
    stack_version: string;
    tags: any[];
    unique: boolean;
  };
}

type ClusterStackVersion = {
  cluster_name: string;
  id: number;
  repository_summary: {
    services: {
      [key: string]: {
        version: string;
        release_version: string;
        upgrade: boolean;
      };
    };
  };
  repository_version: number;
  stack: string;
  state: string;
  supports_revert: boolean;
  version: string;
  host_states: {
    CURRENT: string[];
    INSTALLED: string[];
    INSTALLING: string[];
    INSTALL_FAILED: string[];
    NOT_REQUIRED: string[];
    OUT_OF_SYNC: string[];
  };
}

type StackVersion = {
  href: string;
  ClusterStackVersions: ClusterStackVersion;
  repository_versions: RepositoryVersion[];
}

export type {
    RepositoryVersion,
    Service,
    Version,
    StackService,
    OperatingSystem,
    Repository,
    ClusterStackVersion,
    StackVersion,
};