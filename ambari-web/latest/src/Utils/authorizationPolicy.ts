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

const UPGRADE_PERMISSION_EXCEPTIONS = new Set([
  "CLUSTER.MANAGE_USER_PERSISTED_DATA",
  "CLUSTER.UPGRADE_DOWNGRADE_STACK",
]);

export function splitPermissionExpression(expression: string): string[] {
  return expression.split(",").map((permission) => permission.trim()).filter(Boolean);
}

export function permissionExpressionHasUpgradeException(expression: string): boolean {
  return splitPermissionExpression(expression).some((permission) => (
    UPGRADE_PERMISSION_EXCEPTIONS.has(permission)
  ));
}

export function canUsePermissionExpression({
  expression,
  hasRawPermission,
  operationsAllowedDuringUpgrade,
  upgradeIsBlocking,
}: {
  expression: string;
  hasRawPermission: (expression: string) => boolean;
  operationsAllowedDuringUpgrade: boolean;
  upgradeIsBlocking: boolean;
}): boolean {
  if (
    upgradeIsBlocking
    && !operationsAllowedDuringUpgrade
    && !permissionExpressionHasUpgradeException(expression)
  ) {
    return false;
  }
  return hasRawPermission(expression);
}

export function canRunAuthorizedOperation({
  isNonWizardUser,
  ...permissionOptions
}: Parameters<typeof canUsePermissionExpression>[0] & { isNonWizardUser: boolean }): boolean {
  return !isNonWizardUser && canUsePermissionExpression(permissionOptions);
}
