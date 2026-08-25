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
import { canEnterAdminView } from "./AdminViewRouteGuard";

describe("Admin View route policy", () => {
  it("keeps the classic stack-upgrade guard for an existing cluster", () => {
    expect(canEnterAdminView({
      adminPage: null,
      canManageStackVersions: true,
      canUpgradeStack: false,
      clusterName: "c1",
      noClusterLanding: false,
      viewOnly: false,
    })).toBe(false);
    expect(canEnterAdminView({
      adminPage: null,
      canManageStackVersions: false,
      canUpgradeStack: true,
      clusterName: "c1",
      noClusterLanding: false,
      viewOnly: false,
    })).toBe(true);
  });

  it("allows only the post-login no-cluster Admin View transition", () => {
    expect(canEnterAdminView({
      adminPage: null,
      canManageStackVersions: false,
      canUpgradeStack: false,
      clusterName: "",
      noClusterLanding: true,
      viewOnly: false,
    })).toBe(true);
    expect(canEnterAdminView({
      adminPage: null,
      canManageStackVersions: false,
      canUpgradeStack: false,
      clusterName: "",
      noClusterLanding: false,
      viewOnly: false,
    })).toBe(false);
    expect(canEnterAdminView({
      adminPage: null,
      canManageStackVersions: false,
      canUpgradeStack: false,
      clusterName: "",
      noClusterLanding: true,
      viewOnly: true,
    })).toBe(false);
  });

  it("uses the Manage Stack Versions policy for that Admin View page", () => {
    expect(canEnterAdminView({
      adminPage: "stackVersions",
      canManageStackVersions: true,
      canUpgradeStack: false,
      clusterName: "c1",
      noClusterLanding: false,
      viewOnly: false,
    })).toBe(true);
    expect(canEnterAdminView({
      adminPage: "stackVersions",
      canManageStackVersions: false,
      canUpgradeStack: true,
      clusterName: "c1",
      noClusterLanding: false,
      viewOnly: false,
    })).toBe(false);
  });
});
