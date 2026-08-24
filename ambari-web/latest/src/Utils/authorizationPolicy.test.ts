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

import { describe, expect, it, vi } from "vitest";
import {
  canRunAuthorizedOperation,
  canUsePermissionExpression,
  permissionExpressionHasUpgradeException,
  splitPermissionExpression,
} from "./authorizationPolicy";

describe("authorization policy", () => {
  it("normalizes comma-separated OR expressions", () => {
    expect(splitPermissionExpression(" A, B ,, C ")).toEqual(["A", "B", "C"]);
  });

  it("recognizes the two classic upgrade exceptions", () => {
    expect(permissionExpressionHasUpgradeException(
      "SERVICE.START_STOP, CLUSTER.MANAGE_USER_PERSISTED_DATA",
    )).toBe(true);
    expect(permissionExpressionHasUpgradeException("SERVICE.START_STOP")).toBe(false);
  });

  it("blocks ordinary permissions during an upgrade before querying RBAC", () => {
    const hasRawPermission = vi.fn(() => true);
    expect(canUsePermissionExpression({
      expression: "SERVICE.START_STOP",
      hasRawPermission,
      operationsAllowedDuringUpgrade: false,
      upgradeIsBlocking: true,
    })).toBe(false);
    expect(hasRawPermission).not.toHaveBeenCalled();
  });

  it("preserves classic whole-expression upgrade exception behavior", () => {
    expect(canUsePermissionExpression({
      expression: "SERVICE.START_STOP, CLUSTER.UPGRADE_DOWNGRADE_STACK",
      hasRawPermission: () => true,
      operationsAllowedDuringUpgrade: false,
      upgradeIsBlocking: true,
    })).toBe(true);
  });

  it("allows the support flag but still requires the requested RBAC permission", () => {
    expect(canUsePermissionExpression({
      expression: "SERVICE.START_STOP",
      hasRawPermission: () => false,
      operationsAllowedDuringUpgrade: true,
      upgradeIsBlocking: true,
    })).toBe(false);
    expect(canUsePermissionExpression({
      expression: "SERVICE.START_STOP",
      hasRawPermission: () => true,
      operationsAllowedDuringUpgrade: true,
      upgradeIsBlocking: true,
    })).toBe(true);
  });

  it("allows an authorized mutation when no global workflow blocks it", () => {
    expect(canRunAuthorizedOperation({
      expression: "SERVICE.MODIFY_CONFIGS",
      hasRawPermission: () => true,
      isNonWizardUser: false,
      operationsAllowedDuringUpgrade: false,
      upgradeIsBlocking: false,
    })).toBe(true);
  });

  it("revokes operation capability for a different wizard owner", () => {
    expect(canRunAuthorizedOperation({
      expression: "SERVICE.ENABLE_HA",
      hasRawPermission: () => true,
      isNonWizardUser: true,
      operationsAllowedDuringUpgrade: true,
      upgradeIsBlocking: false,
    })).toBe(false);
  });

  it("keeps read-only permission visibility for a different wizard owner", () => {
    const options = {
      expression: "SERVICE.COMPARE_CONFIGS",
      hasRawPermission: () => true,
      operationsAllowedDuringUpgrade: false,
      upgradeIsBlocking: false,
    };

    expect(canUsePermissionExpression(options)).toBe(true);
    expect(canRunAuthorizedOperation({
      ...options,
      isNonWizardUser: true,
    })).toBe(false);
  });
});
