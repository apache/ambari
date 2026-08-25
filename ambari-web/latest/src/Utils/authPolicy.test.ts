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
import {
  clusterNavigationEnabled,
  clusterProvisioningRedirect,
  hasVersionConflict,
  isViewOnlyUser,
  selectLandingPath,
  shouldUseMinimalViewsShell,
} from "./authPolicy";

describe("application landing policy", () => {
  it.each([
    [{ clusterInstalled: true, clusterName: "c1", viewOnly: false }, "/main/dashboard/metrics"],
    [{
      clusterInstalled: true,
      clusterName: "c1",
      preferredPath: "/main/hosts",
      viewOnly: false,
    }, "/main/hosts"],
    [{ clusterInstalled: false, clusterName: "c1", viewOnly: false }, "/installer/step0"],
    [{ clusterInstalled: false, clusterName: "", viewOnly: false }, "/adminView"],
    [{ clusterInstalled: true, clusterName: "c1", viewOnly: true }, "/main/view"],
    [{ clusterInstalled: false, clusterName: "", viewOnly: true }, "/main/view"],
  ])("selects the expected landing path", (input, expected) => {
    expect(selectLandingPath(input)).toBe(expected);
  });

  it("classifies only empty or VIEW.USE-only authorization sets as View-only", () => {
    expect(isViewOnlyUser([])).toBe(true);
    expect(isViewOnlyUser([{ authorization_id: "VIEW.USE" } as never])).toBe(true);
    expect(isViewOnlyUser([{ authorization_id: "CLUSTER.VIEW_METRICS" } as never])).toBe(false);
    expect(isViewOnlyUser([
      { authorization_id: "VIEW.USE" } as never,
      { authorization_id: "AMBARI.MANAGE_VIEWS" } as never,
    ])).toBe(false);
  });

  it("removes cluster navigation from Installer and View-only shells", () => {
    expect(clusterNavigationEnabled(true, "/main/dashboard/metrics")).toBe(true);
    expect(clusterNavigationEnabled(false, "/main/view/TEZ/tez")).toBe(false);
    expect(clusterNavigationEnabled(true, "/installer/step3")).toBe(false);
  });

  it.each([
    [{
      canAddDeleteClusters: true,
      clusterInstalled: true,
      clusterName: "c1",
      pathname: "/installer/step3",
    }, "/main/dashboard/metrics"],
    [{
      canAddDeleteClusters: false,
      clusterInstalled: true,
      clusterName: "c1",
      pathname: "/installer/step3",
    }, "/main/view"],
    [{
      canAddDeleteClusters: false,
      clusterInstalled: false,
      clusterName: "c1",
      pathname: "/installer/step3",
    }, "/main/view"],
    [{
      canAddDeleteClusters: true,
      clusterInstalled: false,
      clusterName: "c1",
      pathname: "/main/dashboard/metrics",
    }, "/installer/step0"],
    [{
      canAddDeleteClusters: false,
      clusterInstalled: false,
      clusterName: "c1",
      pathname: "/main/dashboard/metrics",
    }, "/main/view"],
    [{
      canAddDeleteClusters: false,
      clusterInstalled: false,
      clusterName: "c1",
      pathname: "/main/view/TEZ/tez",
    }, null],
    [{
      canAddDeleteClusters: false,
      clusterInstalled: false,
      clusterName: "c1",
      pathname: "/main/views/TEZ/1.0/INSTANCE",
    }, null],
  ])("applies the incomplete-cluster Views/Installer policy", (input, expected) => {
    expect(clusterProvisioningRedirect(input)).toBe(expected);
  });

  it.each([
    [{ clusterInstalled: true, viewOnly: false, viewRoute: true }, false],
    [{ clusterInstalled: false, viewOnly: false, viewRoute: true }, true],
    [{ clusterInstalled: undefined, viewOnly: true, viewRoute: true }, true],
    [{ clusterInstalled: false, viewOnly: false, viewRoute: false }, false],
  ])("selects the minimal Views shell only when cluster controls are unavailable", (input, expected) => {
    expect(shouldUseMinimalViewsShell(input)).toBe(expected);
  });

  it.each([
    ["", "2.0.0", false],
    ["2.0.0", "", false],
    ["2.0.0", "2.0.0", false],
    ["2.0.0", "2.0.1", true],
  ])("compares packaged and server versions", (client, server, expected) => {
    expect(hasVersionConflict(client, server)).toBe(expected);
  });
});
