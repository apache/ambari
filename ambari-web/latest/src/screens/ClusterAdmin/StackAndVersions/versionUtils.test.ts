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

import { describe, expect, it } from "vitest";
import type { StackVersion } from "./types";
import {
  canHideRepositoryVersion,
  compatibleRepositoryVersionNames,
  filterVisibleStackVersions,
  versionMatchesFilter,
  type VersionFilterKey,
} from "./versionUtils";

function stack(
  repositoryVersion: string,
  state: string,
  options: { stackName?: string; type?: string; hidden?: boolean; displayName?: string } = {},
): StackVersion {
  return {
    href: "",
    ClusterStackVersions: {
      cluster_name: "c1",
      id: Number(repositoryVersion.replace(/\D/g, "")) || 1,
      repository_summary: { services: {} },
      repository_version: 1,
      stack: options.stackName || "HDP",
      state,
      supports_revert: false,
      revert_upgrade_id: null,
      version: "3.1",
      host_states: {
        CURRENT: [], INSTALLED: [], INSTALLING: [], INSTALL_FAILED: [],
        NOT_REQUIRED: [], OUT_OF_SYNC: [],
      },
    },
    repository_versions: [{
      href: "",
      RepositoryVersions: {
        display_name: options.displayName || repositoryVersion,
        has_children: false,
        hidden: options.hidden || false,
        id: 1,
        parent_id: null,
        repository_version: repositoryVersion,
        resolved: true,
        services: [],
        stack_name: options.stackName || "HDP",
        stack_services: [],
        stack_version: "3.1",
        type: options.type || "STANDARD",
        release: { build: null, compatible_with: null, notes: "", version: repositoryVersion },
      },
      operating_systems: [],
    }],
  };
}

describe("stack version visibility", () => {
  it("honors hidden, older-version, patch, maintenance, and compatibility rules", () => {
    const current = stack("3.1.5", "CURRENT");
    const versions = [
      current,
      stack("3.1.4", "INSTALLED"),
      stack("3.1.4.1", "INSTALLED", { type: "PATCH" }),
      stack("3.1.3.1", "INSTALLED", { type: "MAINT" }),
      stack("4.0.0", "INSTALLED", { stackName: "HDF" }),
      stack("4.1.0", "INSTALLED", { stackName: "HDF" }),
      stack("3.2.0", "NOT_REQUIRED", { hidden: true }),
    ];

    expect(filterVisibleStackVersions(versions, current, new Set(["4.0.0"]), false))
      .toEqual([current, versions[2], versions[3], versions[4]]);
    expect(filterVisibleStackVersions(versions, current, new Set(), true))
      .toEqual(versions.slice(0, 6));
  });

  it("extracts only valid compatible repository versions", () => {
    expect(compatibleRepositoryVersionNames({ items: [
      { CompatibleRepositoryVersions: { repository_version: "3.2.0" } },
      { CompatibleRepositoryVersions: {} },
    ] })).toEqual(new Set(["3.2.0"]));
  });
});

describe("stack version filters", () => {
  const current = stack("3.1.5", "CURRENT");
  const cases: Array<[VersionFilterKey, StackVersion, boolean]> = [
    ["NOT INSTALLED", stack("3.2.0", "NOT_REQUIRED"), true],
    ["NOT INSTALLED", stack("3.2.0", "OUT_OF_SYNC"), true],
    ["UPGRADE READY", stack("3.2.0", "INSTALLED"), true],
    ["INSTALLED", stack("3.1.4", "INSTALLED"), true],
    ["UPGRADE/DOWNGRADE IN PROGRESS", stack("3.2.0", "INSTALLED", { displayName: "target" }), true],
    ["UPGRADE/DOWNGRADE IN PROGRESS", stack("3.2.0", "UPGRADING"), true],
    ["READY TO FINALIZE", stack("3.2.0", "UPGRADED"), true],
  ];

  it.each(cases)("matches %s", (filter, candidate, expected) => {
    expect(versionMatchesFilter(candidate, filter, current, "target")).toBe(expected);
  });

  it("does not report the active target as upgrade ready", () => {
    expect(versionMatchesFilter(
      stack("3.2.0", "INSTALLED", { displayName: "target" }),
      "UPGRADE READY",
      current,
      "target",
    )).toBe(false);
  });
});

describe("repository version hide eligibility", () => {
  it("allows unused, failed, and superseded patch or maintenance versions", () => {
    expect(canHideRepositoryVersion(stack("3.2.0", "NOT_REQUIRED"))).toBe(true);
    expect(canHideRepositoryVersion(stack("3.2.0", "INSTALL_FAILED"))).toBe(true);
    expect(canHideRepositoryVersion(stack("3.1.5.1", "INSTALLED", { type: "PATCH" }))).toBe(true);
    expect(canHideRepositoryVersion(stack("3.1.5", "CURRENT"))).toBe(false);
    expect(canHideRepositoryVersion(stack("3.1.4", "INSTALLED"))).toBe(false);
  });
});
