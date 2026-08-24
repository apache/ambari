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

export async function redirectToAdminView(adminPage = "") {
  const suffix = adminPage ? `?page=${encodeURIComponent(adminPage)}` : "";
  window.location.hash = `/adminView${suffix}`;
}

export function applicationRootFromDocumentPath(documentPath: string): string {
  const path = documentPath.startsWith("/") ? documentPath : `/${documentPath}`;
  let applicationRoot = path.endsWith("/")
    ? path
    : path.slice(0, path.lastIndexOf("/") + 1);

  if (applicationRoot.endsWith("/latest/")) {
    applicationRoot = applicationRoot.slice(0, -"latest/".length);
  }
  return applicationRoot || "/";
}

export function classicExperienceUrl(documentPath: string): string {
  return `${applicationRootFromDocumentPath(documentPath)}#/`;
}

type ServerVersionResponse = {
  components?: Array<{
    RootServiceComponents?: { component_version?: string };
  }>;
};

function compareVersionParts(left: string, right: string): number {
  const leftParts = left.match(/\d+/g)?.map(Number) || [];
  const rightParts = right.match(/\d+/g)?.map(Number) || [];
  const length = Math.max(leftParts.length, rightParts.length);
  for (let index = 0; index < length; index += 1) {
    const difference = (leftParts[index] || 0) - (rightParts[index] || 0);
    if (difference) return difference;
  }
  return left.localeCompare(right);
}

export function latestServerVersion(data: ServerVersionResponse): string | null {
  const versions = (data?.components || [])
    .map((item) => item.RootServiceComponents?.component_version)
    .filter((value): value is string => Boolean(value))
    .sort(compareVersionParts);
  return versions.at(-1)
    ?.replace(/[^\d.-]/g, "")
    .replace(/[.-]+$/, "") || null;
}

export function adminViewUrl(
  version: string,
  page: string | null,
  documentPath: string,
): string {
  const applicationRoot = applicationRootFromDocumentPath(documentPath);
  const adminHash = page ? `#/${encodeURIComponent(page)}` : "#/";
  return `${applicationRoot}views/ADMIN_VIEW/${encodeURIComponent(version)}/INSTANCE/${adminHash}`;
}
