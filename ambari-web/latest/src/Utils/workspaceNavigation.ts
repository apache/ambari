/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export interface WorkspaceReturn {
  schema_version: 1;
  username: string;
  path: string;
  clusterName: string;
}

const storageKey = (username: string) => "ambari.workspace.return." + encodeURIComponent(username);

export function workspaceReturn(username: string, path: string): WorkspaceReturn | null {
  if (!username || typeof path !== "string" || path.length > 65536) return null;
  const match = path.match(/^\/clusters\/([^/?#]+)\/main(?:\/[^?#]*)?(?:\?[^#]*)?$/);
  if (!match) return null;
  try {
    const parsed = new URL(path, "https://ambari.invalid");
    if (parsed.origin !== "https://ambari.invalid" || parsed.hash
      || parsed.pathname !== path.split("?")[0]) return null;
    const segments = parsed.pathname.split("/").map(decodeURIComponent);
    if (segments.some(segment => segment === "." || segment === "..")) return null;
    return { schema_version: 1, username, path, clusterName: decodeURIComponent(match[1]) };
  } catch {
    return null;
  }
}

export function rememberWorkspace(username: string, path: string): WorkspaceReturn | null {
  const target = workspaceReturn(username, path);
  if (target) {
    try { sessionStorage.setItem(storageKey(username), JSON.stringify(target)); } catch { /* Navigation still works with browser storage disabled. */ }
  }
  return target;
}

export function readWorkspace(username: string): WorkspaceReturn | null {
  if (!username) return null;
  try {
    const value = JSON.parse(sessionStorage.getItem(storageKey(username)) || "null");
    if (value?.schema_version !== 1 || value.username !== username || typeof value.path !== "string") return null;
    return workspaceReturn(username, value.path);
  } catch {
    return null;
  }
}
