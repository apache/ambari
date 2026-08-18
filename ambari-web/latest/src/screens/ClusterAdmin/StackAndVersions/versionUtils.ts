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

import stringUtilsObj from "../../../Utils/StringUtilsObj";
import type { RepositoryVersion, StackVersion } from "./types";

export type VersionFilterKey =
  | "ALL"
  | "NOT INSTALLED"
  | "UPGRADE READY"
  | "CURRENT"
  | "INSTALLED"
  | "UPGRADE/DOWNGRADE IN PROGRESS"
  | "READY TO FINALIZE";

export function getRepositoryVersion(stack: StackVersion): RepositoryVersion | undefined {
  return stack.repository_versions?.[0];
}

export function compatibleRepositoryVersionNames(response: unknown): Set<string> {
  const items = (response as { items?: Array<{ CompatibleRepositoryVersions?: { repository_version?: string } }> })?.items;
  return new Set(
    (items || [])
      .map((item) => item.CompatibleRepositoryVersions?.repository_version)
      .filter((version): version is string => Boolean(version)),
  );
}

export function filterVisibleStackVersions(
  stacks: StackVersion[],
  currentStack: StackVersion | undefined,
  compatibleVersions: Set<string>,
  displayOlderVersions: boolean,
): StackVersion[] {
  const currentRepository = currentStack && getRepositoryVersion(currentStack)?.RepositoryVersions;

  return stacks.filter((stack) => {
    const repository = getRepositoryVersion(stack)?.RepositoryVersions;
    if (!repository || repository.hidden) return false;
    if (displayOlderVersions || !currentRepository) return true;
    if (stack.ClusterStackVersions.state === "CURRENT") return true;

    if (repository.stack_name === currentRepository.stack_name) {
      return repository.type === "PATCH"
        || repository.type === "MAINT"
        || stringUtilsObj.compareVersions(
          String(repository.repository_version),
          String(currentRepository.repository_version),
        ) >= 0;
    }

    return compatibleVersions.has(repository.repository_version);
  });
}

export function versionMatchesFilter(
  stack: StackVersion,
  filter: VersionFilterKey,
  currentStack: StackVersion | undefined,
  activeUpgradeDisplayName?: string,
): boolean {
  if (filter === "ALL") return true;

  const state = stack.ClusterStackVersions.state;
  const repository = getRepositoryVersion(stack)?.RepositoryVersions;
  const currentRepository = currentStack && getRepositoryVersion(currentStack)?.RepositoryVersions;
  const isActiveUpgrade = Boolean(
    activeUpgradeDisplayName && repository?.display_name === activeUpgradeDisplayName,
  );

  if (filter === "NOT INSTALLED") {
    return ["NOT_REQUIRED", "INSTALL_FAILED", "INSTALLING", "OUT_OF_SYNC"].includes(state);
  }
  if (filter === "CURRENT") return state === "CURRENT";
  if (filter === "UPGRADE/DOWNGRADE IN PROGRESS") {
    return state === "UPGRADING" || (state === "INSTALLED" && isActiveUpgrade);
  }
  if (filter === "READY TO FINALIZE") return state === "UPGRADED";

  if (state !== "INSTALLED" || !repository || !currentRepository) return false;
  const comparison = stringUtilsObj.compareVersions(
    String(repository.repository_version),
    String(currentRepository.repository_version),
  );
  if (filter === "UPGRADE READY") return !isActiveUpgrade && comparison > 0;
  if (filter === "INSTALLED") return comparison <= 0;
  return false;
}

export function canHideRepositoryVersion(stack: StackVersion): boolean {
  const state = stack.ClusterStackVersions.state;
  const type = getRepositoryVersion(stack)?.RepositoryVersions.type;
  return state === "NOT_REQUIRED"
    || state === "INSTALL_FAILED"
    || (state === "INSTALLED" && (type === "PATCH" || type === "MAINT"));
}
