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
};

type Service = {
  name: string;
  versions: Version[];
  display_name: string;
};

type Version = {
  version: string;
  components: any[];
};

type StackService = {
  name: string;
  display_name: string;
  comment: string;
  versions: string[];
};

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
};

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
};

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
  revert_upgrade_id: number | null;
  version: string;
  host_states: {
    CURRENT: string[];
    INSTALLED: string[];
    INSTALLING: string[];
    INSTALL_FAILED: string[];
    NOT_REQUIRED: string[];
    OUT_OF_SYNC: string[];
  };
};

type StackVersion = {
  href: string;
  ClusterStackVersions: ClusterStackVersion;
  repository_versions: RepositoryVersion[];
};

type UpgradeCheck = {
  check: string;
  check_type: string;
  cluster_name: string;
  failed_detail: Array<{
    state: string;
    label: string;
    host_name: string;
  }>;
  failed_on: string[];
  id: string;
  reason: string;
  repository_version_id: number;
  status: string;
  upgrade_type: string;
};

type Item = {
  href: string;
  UpgradeChecks: UpgradeCheck;
};

type Response = {
  href: string;
  items: Item[];
};

type ClusterCheckPopupData = {
  header: any;
  failTitle: any;
  failAlert: any;
  warningTitle: any;
  warningAlert: any;
  primary: any;
  secondary: any;
  bypassedFailures: boolean;
};

type TaskLog = {
  Tasks: {
    attempt_cnt: number;
    cluster_name: string;
    command: string;
    command_detail: string;
    custom_command_name: string;
    end_time: number;
    error_log: string;
    exit_code: number;
    host_name: string;
    id: number;
    ops_display_name: string | null;
    output_log: string;
    request_id: number;
    role: string;
    stage_id: number;
    start_time: number;
    status: string;
    stderr: string;
    stdout: string;
    structured_out: {
      direction: string;
      repository_version_id: number;
      upgrade_type: string;
      version: string;
    };
  };
};

type Task = {
  command_detail: string;
  host_name: string;
  id: number;
  request_id: number;
  role: string;
  stage_id: number;
  status: string;
  structured_out: {
    direction: string;
    repository_version_id: number;
    upgrade_type: string;
    version: string;
  };
  logs?: TaskLog;
};

type UpgradeItem = {
  UpgradeItem: {
    context: string;
    display_status: string;
    group_id: number;
    progress_percent: number;
    request_id: number;
    skippable: boolean;
    stage_id: number;
    status: string;
    text: string;
  };
  tasks?: Task[];
};

type UpgradeGroup = {
  [x: string]: any;
  UpgradeGroup: {
    completed_task_count: number;
    display_status: string;
    group_id: number;
    in_progress_task_count: number;
    name: string;
    progress_percent: number;
    request_id: number;
    status: string;
    title: string;
    total_task_count: number;
  };
  upgrade_items: UpgradeItem[];
};

type Upgrade = {
  associated_version: string;
  cluster_name: string;
  create_time: number;
  direction: string;
  downgrade_allowed: boolean;
  end_time: number;
  exclusive: boolean;
  pack: string;
  progress_percent: number;
  request_context: string;
  request_id: number;
  request_status: string;
  skip_failures: boolean;
  skip_service_check_failures: boolean;
  start_time: number;
  suspended: boolean;
  type: string;
  upgrade_id: number;
  upgrade_type: string;
  versions: Record<string, any>;
};

type UpgradeData = {
  Upgrade: Upgrade;
  upgrade_groups: UpgradeGroup[];
};

type UpgradeParameters = {
  isDowngrade: boolean;
  downgradeAllowed: boolean;
  isDowngradeAvailable?: boolean;
  overallProgress: number;
  activeGroup: UpgradeGroup | null;
  runningItem: UpgradeItem | null;
  failedItem: UpgradeItem | null;
  manualItem: UpgradeItem | null;
  plainManualItem: boolean;
  isSlaveComponentFailuresItem: boolean;
  isServiceCheckFailuresItem: boolean;
  isFinalizeItem: boolean;
  canSkipFailedItem: boolean;
  isHoldingState: boolean;
  requestInProgress: boolean;
  areSlaveComponentFailuresHostsLoaded: boolean;
  slaveComponentStructuredInfo: any;
  areServiceCheckFailuresServicenamesLoaded?: boolean;
  serviceCheckFailuresServicenames?: any;
  upgradeStatus: string;
  upgradeInit: boolean;
  upgradeInProgress: boolean;
  upgradeCompleted: boolean;
  upgradeSuspended: boolean;
  upgradeAborted: boolean;
  upgradeHolding: boolean;
  upgradeRunning: boolean;
  showPauseButton: boolean;
  upgradeStatusLabel: string;
  upgradeAssociatedversion: string;
  slaveComponentFailures: boolean;
  serviceCheckFailures: boolean;
  upgradeMethod: string;
};

export type {
  RepositoryVersion,
  Service,
  Version,
  StackService,
  OperatingSystem,
  Repository,
  ClusterStackVersion,
  StackVersion,
  UpgradeCheck,
  Item,
  Response,
  ClusterCheckPopupData,
  UpgradeData,
  UpgradeGroup,
  UpgradeItem,
  Task,
  UpgradeParameters,
};