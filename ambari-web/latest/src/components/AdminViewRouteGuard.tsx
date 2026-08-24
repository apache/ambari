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
import { Navigate, useLocation } from "react-router-dom";
import { isViewOnlyUser } from "../Utils/authPolicy";
import { useAuth } from "../hooks/useAuth";
import useAuthorizationPolicy from "../hooks/useAuthorizationPolicy";
import { AppContext } from "../store/context";

export function canEnterAdminView({
  adminPage,
  canManageStackVersions,
  canUpgradeStack,
  clusterName,
  noClusterLanding,
  viewOnly,
}: {
  adminPage: string | null;
  canManageStackVersions: boolean;
  canUpgradeStack: boolean;
  clusterName: string;
  noClusterLanding: boolean;
  viewOnly: boolean;
}): boolean {
  if (adminPage === "stackVersions") {
    return canManageStackVersions && !viewOnly;
  }
  return canUpgradeStack || (!clusterName && noClusterLanding && !viewOnly);
}

export default function AdminViewRouteGuard({ children }: { children: ReactNode }) {
  const { clusterName } = useContext(AppContext);
  const location = useLocation();
  const { authorizations } = useAuth();
  const { isAuthorized } = useAuthorizationPolicy();
  return canEnterAdminView({
    adminPage: new URLSearchParams(location.search).get("page"),
    canManageStackVersions: isAuthorized("AMBARI.MANAGE_STACK_VERSIONS"),
    canUpgradeStack: isAuthorized("CLUSTER.UPGRADE_DOWNGRADE_STACK"),
    clusterName,
    noClusterLanding: Boolean(
      (location.state as { noClusterLanding?: boolean } | null)?.noClusterLanding,
    ),
    viewOnly: isViewOnlyUser(authorizations),
  })
    ? children
    : <Navigate to="/" replace />;
}
