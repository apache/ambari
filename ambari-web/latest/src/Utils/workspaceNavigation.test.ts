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

import { describe, expect, it, vi } from "vitest";
import { readWorkspace, rememberWorkspace, workspaceReturn } from "./workspaceNavigation";

describe("global workspace return", () => {
  it("restores the exact cluster page and query without crossing user sessions", () => {
    const path = "/clusters/team_a/main/services/DORIS/configs?version=6&group=Default";
    rememberWorkspace("alice", path);
    expect(readWorkspace("alice")).toMatchObject({ path, clusterName: "team_a" });
    expect(readWorkspace("bob")).toBeNull();
    rememberWorkspace("alice", "/mpacks");
    expect(readWorkspace("alice")?.path).toBe(path);
  });
  it("rejects external URLs, normalized traversal, malformed paths, and foreign ownership", () => {
    for (const path of ["https://outside.invalid/", "//outside.invalid/", "/clusters/c/main/../../mpacks",
      "/clusters/c/main/%2e%2e/admin", "/clusters/c/main/dashboard#outside", "/clusters/%xx/main/dashboard"]) {
      expect(workspaceReturn("alice", path)).toBeNull();
    }
    sessionStorage.setItem("ambari.workspace.return.alice", JSON.stringify({
      schema_version: 1, username: "bob", path: "/clusters/c/main/dashboard",
    }));
    expect(readWorkspace("alice")).toBeNull();
  });
  it("tolerates unavailable storage without blocking navigation", () => {
    const spy = vi.spyOn(sessionStorage, "setItem").mockImplementation(() => { throw new Error("blocked"); });
    expect(rememberWorkspace("alice", "/clusters/c/main/dashboard")?.clusterName).toBe("c");
    spy.mockRestore();
  });
});
