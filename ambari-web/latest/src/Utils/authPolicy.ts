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

import { Authorization } from "../types/auth";

export function isViewOnlyUser(authorizations: Authorization[]): boolean {
  return authorizations.length === 0
    || (authorizations.length === 1
      && authorizations[0].authorization_id === "VIEW.USE");
}

export function clusterNavigationEnabled(
  clusterControls: boolean,
  pathname: string,
): boolean {
  return clusterControls
    && !pathname.includes("installer")
    && !pathname.includes("install");
}

export function clusterProvisioningRedirect({
  canAddDeleteClusters,
  clusterInstalled,
  clusterName,
  pathname,
}: {
  canAddDeleteClusters: boolean;
  clusterInstalled: boolean | undefined;
  clusterName?: string;
  pathname: string;
}): string | null {
  if (pathname.startsWith("/installer")) {
    if (!canAddDeleteClusters) {
      return "/main/view";
    }
    if (clusterInstalled === true) {
      return "/main/dashboard/metrics";
    }
  }
  if (
    clusterName
    && clusterInstalled === false
    && pathname.startsWith("/main")
    && !pathname.startsWith("/main/view")
  ) {
    return canAddDeleteClusters ? "/installer/step0" : "/main/view";
  }
  return null;
}

export function shouldUseMinimalViewsShell({
  clusterInstalled,
  viewOnly,
  viewRoute,
}: {
  clusterInstalled: boolean | undefined;
  viewOnly: boolean;
  viewRoute: boolean;
}): boolean {
  return viewOnly || (viewRoute && clusterInstalled !== true);
}

export function selectLandingPath({
  clusterInstalled,
  clusterName,
  preferredPath,
  viewOnly,
}: {
  clusterInstalled: boolean;
  clusterName?: string;
  preferredPath?: string | null;
  viewOnly: boolean;
}): string {
  if (viewOnly) {
    return "/main/view";
  }
  if (clusterInstalled) {
    return preferredPath || "/main/dashboard/metrics";
  }
  if (clusterName) {
    return "/installer/step0";
  }
  return "/adminView";
}

export function hasVersionConflict(clientVersion: string, serverVersion: string): boolean {
  return Boolean(clientVersion && serverVersion && clientVersion !== serverVersion);
}
