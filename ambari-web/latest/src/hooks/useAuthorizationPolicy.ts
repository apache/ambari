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

import { useCallback, useContext } from "react";
import {
  canRunAuthorizedOperation,
  canUsePermissionExpression,
} from "../Utils/authorizationPolicy";
import { AppContext } from "../store/context";
import { useAuth } from "./useAuth";

export default function useAuthorizationPolicy() {
  const { hasAuthorization } = useAuth();
  const {
    isNonWizardUser,
    supports,
    upgradeIsRunning,
    upgradeSuspended,
  } = useContext(AppContext);
  const operationsAllowedDuringUpgrade = Boolean(supports?.opsDuringRollingUpgrade);
  const upgradeIsBlocking = upgradeIsRunning && !upgradeSuspended;

  const havePermissions = useCallback((expression: string) => (
    canUsePermissionExpression({
      expression,
      hasRawPermission: hasAuthorization,
      operationsAllowedDuringUpgrade,
      upgradeIsBlocking,
    })
  ), [
    hasAuthorization,
    operationsAllowedDuringUpgrade,
    upgradeIsBlocking,
  ]);

  const isAuthorized = useCallback((expression: string) => (
    canRunAuthorizedOperation({
      expression,
      hasRawPermission: hasAuthorization,
      isNonWizardUser,
      operationsAllowedDuringUpgrade,
      upgradeIsBlocking,
    })
  ), [
    hasAuthorization,
    isNonWizardUser,
    operationsAllowedDuringUpgrade,
    upgradeIsBlocking,
  ]);

  return { havePermissions, isAuthorized };
}
