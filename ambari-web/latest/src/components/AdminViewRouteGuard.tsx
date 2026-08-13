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

import { ReactNode, useContext } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import { AppContext } from "../store/context";

export function canEnterAdminView({
  ambariAdmin,
  canUpgradeStack,
  clusterName,
}: {
  ambariAdmin: boolean;
  canUpgradeStack: boolean;
  clusterName: string;
}): boolean {
  return canUpgradeStack || (!clusterName && ambariAdmin);
}

export default function AdminViewRouteGuard({ children }: { children: ReactNode }) {
  const { clusterName } = useContext(AppContext);
  const { hasAuthorization, isAdmin } = useAuth();
  return canEnterAdminView({
    ambariAdmin: isAdmin(),
    canUpgradeStack: hasAuthorization("CLUSTER.UPGRADE_DOWNGRADE_STACK"),
    clusterName,
  })
    ? children
    : <Navigate to="/" replace />;
}
