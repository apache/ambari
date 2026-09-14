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

const DEFAULT_MAIN_PATH = "/main/dashboard/metrics";

function hasMainBoundary(pathname: string): boolean {
  return pathname === "/main" || pathname.startsWith("/main/");
}

export function normalizeLegacyMainPath(path: string): string | null {
  const rawPathname = path.split(/[?#]/, 1)[0];
  if (!hasMainBoundary(rawPathname)) return null;
  try {
    const rawSegments = rawPathname.split("/");
    if (rawSegments.some((segment) => {
      const decoded = decodeURIComponent(segment);
      return decoded === "." || decoded === "..";
    })) return null;
    const parsed = new URL(path, "http://ambari.invalid");
    if (parsed.origin !== "http://ambari.invalid" || parsed.hash || !hasMainBoundary(parsed.pathname)) {
      return null;
    }
    return `${parsed.pathname}${parsed.search}`;
  } catch {
    return null;
  }
}

export function safeLegacyMainPath(
  path: string | null | undefined,
  fallback = DEFAULT_MAIN_PATH,
): string {
  return path ? normalizeLegacyMainPath(path) ?? fallback : fallback;
}

export function clusterPath(
  clusterName: string,
  path = DEFAULT_MAIN_PATH,
): string {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  return `/clusters/${encodeURIComponent(clusterName)}${normalizedPath}`;
}

export function clusterNameFromPath(pathname: string): string | null {
  const match = pathname.match(/^\/clusters\/([^/]+)(?:\/|$)/);
  if (!match) return null;
  try {
    return decodeURIComponent(match[1]);
  } catch {
    return null;
  }
}

export function scopeClusterPath(path: string, clusterName?: string | null): string {
  if (!clusterName) return path;
  const normalizedPath = normalizeLegacyMainPath(path);
  if (normalizedPath) return clusterPath(clusterName, normalizedPath);
  return hasMainBoundary(path.split(/[?#]/, 1)[0])
    ? clusterPath(clusterName, DEFAULT_MAIN_PATH)
    : path;
}

export function legacyMainContinuation(pathname: string, search = ""): string {
  return safeLegacyMainPath(`${pathname}${search}`);
}

export function clusterHashPath(clusterName: string, path: string): string {
  if (!clusterName) throw new Error("Cluster navigation requires an explicit cluster.");
  return `/#${scopeClusterPath(path, clusterName)}`;
}
